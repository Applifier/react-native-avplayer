#import "RCTConvert.h"
#import "RCTAVPlayer.h"
#import "RCTBridgeModule.h"
#import "RCTEventDispatcher.h"
#import "UIView+React.h"

static NSString *const statusKeyPath = @"status";
static NSString *const playbackLikelyToKeepUpKeyPath = @"playbackLikelyToKeepUp";

@implementation RCTAVPlayer
{
  AVPlayer *_player;
  AVPlayerItem *_playerItem;
  BOOL _playerItemObserversSet;
  NSURL *_videoURL;

  /* Required to publish events */
  RCTEventDispatcher *_eventDispatcher;

  bool _pendingSeek;
  float _pendingSeekTime;
  float _lastSeekTime;

  /* For sending videoProgress events */
  id _progressUpdateTimer;
  int _progressUpdateInterval;
  NSDate *_prevProgressUpdateTime;

  /* Keep track of any modifiers, need to be applied after each play */
  float _volume;
  float _rate;
  BOOL _muted;
  BOOL _paused;
  BOOL _repeat;
}

@synthesize uuid;

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{
  if ((self = [super init]))
  {
    _eventDispatcher = eventDispatcher;
    _rate = 1.0;
    _volume = 1.0;
    _pendingSeek = false;
    _pendingSeekTime = 0.0f;
    _lastSeekTime = 0.0f;

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationWillResignActive:)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationWillEnterForeground:)
                                                 name:UIApplicationWillEnterForegroundNotification
                                               object:nil];
  }

  return self;
}

#pragma mark - App lifecycle handlers

- (void)applicationWillResignActive:(NSNotification *)notification
{
  if (!_paused) {
    [self stopProgressTimer];
    [_player setRate:0.0];
  }
}

- (void)applicationWillEnterForeground:(NSNotification *)notification
{
  [self startProgressTimer];
  [self applyModifiers];
}

#pragma mark - Progress

- (void)sendProgressUpdate
{
  AVPlayerItem *video = [_player currentItem];
  if (video == nil || video.status != AVPlayerItemStatusReadyToPlay) {
    return;
  }

  if (_prevProgressUpdateTime == nil || (([_prevProgressUpdateTime timeIntervalSinceNow] * -1000.0) >= _progressUpdateInterval)) {
    [_eventDispatcher sendDeviceEventWithName:@"onVideoProgress"
                                        body:@{@"currentTime": [NSNumber numberWithFloat:CMTimeGetSeconds(video.currentTime)],
                                               @"playableDuration": [self calculatePlayableDuration],
                                               @"target": self.uuid}];
    _prevProgressUpdateTime = [NSDate date];
  }
}

/*!
 * Calculates and returns the playable duration of the current player item using its loaded time ranges.
 *
 * \returns The playable duration of the current player item in seconds.
 */
- (NSNumber *)calculatePlayableDuration
{
  AVPlayerItem *video = _player.currentItem;
  if (video.status == AVPlayerItemStatusReadyToPlay) {
    __block CMTimeRange effectiveTimeRange;
    [video.loadedTimeRanges enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
      CMTimeRange timeRange = [obj CMTimeRangeValue];
      if (CMTimeRangeContainsTime(timeRange, video.currentTime)) {
        effectiveTimeRange = timeRange;
        *stop = YES;
      }
    }];
    Float64 playableDuration = CMTimeGetSeconds(CMTimeRangeGetEnd(effectiveTimeRange));
    if (playableDuration > 0) {
      return [NSNumber numberWithFloat:playableDuration];
    }
  }
  return [NSNumber numberWithInteger:0];
}

- (void)stopProgressTimer
{
  [_progressUpdateTimer invalidate];
}

- (void)startProgressTimer
{
  _progressUpdateInterval = 250;
  _prevProgressUpdateTime = nil;

  [self stopProgressTimer];

  _progressUpdateTimer = [CADisplayLink displayLinkWithTarget:self selector:@selector(sendProgressUpdate)];
  [_progressUpdateTimer addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSDefaultRunLoopMode];
}

- (void)addPlayerItemObservers
{
  [_playerItem addObserver:self forKeyPath:statusKeyPath options:0 context:nil];
  [_playerItem addObserver:self forKeyPath:playbackLikelyToKeepUpKeyPath options:0 context:nil];
  _playerItemObserversSet = YES;
}

/* Fixes https://github.com/brentvatne/react-native-video/issues/43
 * Crashes caused when trying to remove the observer when there is no
 * observer set */
- (void)removePlayerItemObservers
{
  if (_playerItemObserversSet) {
    [_playerItem removeObserver:self forKeyPath:statusKeyPath];
    [_playerItem removeObserver:self forKeyPath:playbackLikelyToKeepUpKeyPath];
    _playerItemObserversSet = NO;
  }
}

#pragma mark - Player and source

- (void)setSrc:(NSDictionary *)source
{
  [self removePlayerItemObservers];
  _playerItem = [self playerItemForSource:source];
  [self addPlayerItemObservers];

  [_player pause];

  _player = [AVPlayer playerWithPlayerItem:_playerItem];
  _player.actionAtItemEnd = AVPlayerActionAtItemEndNone;

  [self applyModifiers];


  [_eventDispatcher sendDeviceEventWithName:@"onVideoLoadStart"
                                      body:@{@"src": @{
                                                 @"uri": [source objectForKey:@"uri"],
                                                 @"type": [source objectForKey:@"type"],
                                                 @"isNetwork":[NSNumber numberWithBool:(bool)[source objectForKey:@"isNetwork"]]},
                                             @"target": self.uuid}];
}

- (AVPlayerItem*)playerItemForSource:(NSDictionary *)source
{
  bool isNetwork = [RCTConvert BOOL:[source objectForKey:@"isNetwork"]];
  bool isAsset = [RCTConvert BOOL:[source objectForKey:@"isAsset"]];
  NSString *uri = [source objectForKey:@"uri"];
  NSString *type = [source objectForKey:@"type"];

  NSURL *url = (isNetwork || isAsset) ?
    [NSURL URLWithString:uri] :
    [[NSURL alloc] initFileURLWithPath:[[NSBundle mainBundle] pathForResource:uri ofType:type]];

  if (isAsset) {
    AVURLAsset *asset = [AVURLAsset URLAssetWithURL:url options:nil];
    return [AVPlayerItem playerItemWithAsset:asset];
  }

  return [AVPlayerItem playerItemWithURL:url];
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context
{
  if (object == _playerItem) {

    if ([keyPath isEqualToString:statusKeyPath]) {
      // Handle player item status change.
      if (_playerItem.status == AVPlayerItemStatusReadyToPlay) {
        float duration = CMTimeGetSeconds(_playerItem.asset.duration);
        if (isnan(duration)) {
          duration = 0.0;
        }
        CGSize size = _playerItem.presentationSize;
        [_eventDispatcher sendDeviceEventWithName:@"onVideoLoad"
                                            body:@{@"duration": [NSNumber numberWithFloat:duration],
                                                   @"currentTime": [NSNumber numberWithFloat:CMTimeGetSeconds(_playerItem.currentTime)],
                                                   @"height": @(size.height),
                                                   @"width": @(size.width),
                                                   @"canPlayReverse": [NSNumber numberWithBool:_playerItem.canPlayReverse],
                                                   @"canPlayFastForward": [NSNumber numberWithBool:_playerItem.canPlayFastForward],
                                                   @"canPlaySlowForward": [NSNumber numberWithBool:_playerItem.canPlaySlowForward],
                                                   @"canPlaySlowReverse": [NSNumber numberWithBool:_playerItem.canPlaySlowReverse],
                                                   @"canStepBackward": [NSNumber numberWithBool:_playerItem.canStepBackward],
                                                   @"canStepForward": [NSNumber numberWithBool:_playerItem.canStepForward],
                                                   @"target": self.uuid}];

        [self startProgressTimer];
        [self attachListeners];
        [self applyModifiers];
      } else if(_playerItem.status == AVPlayerItemStatusFailed) {
        [_eventDispatcher sendDeviceEventWithName:@"onVideoError"
                                            body:@{@"error": @{
                                                       @"code": [NSNumber numberWithInteger: _playerItem.error.code],
                                                       @"domain": _playerItem.error.domain},
                                                   @"target": self.uuid}];
      }
    } else if ([keyPath isEqualToString:playbackLikelyToKeepUpKeyPath]) {
      // Continue playing (or not if paused) after being paused due to hitting an unbuffered zone.
      if (_playerItem.playbackLikelyToKeepUp) {
        [self setPaused:_paused];
      }
    }
  } else {
    [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
  }
}

- (void)attachListeners
{
  // listen for end of file
  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(playerItemDidReachEnd:)
                                               name:AVPlayerItemDidPlayToEndTimeNotification
                                             object:[_player currentItem]];
}

- (void)playerItemDidReachEnd:(NSNotification *)notification
{
  [_eventDispatcher sendDeviceEventWithName:@"onVideoEnd" body:@{@"target": self.uuid}];

  if (_repeat) {
    AVPlayerItem *item = [notification object];
    [item seekToTime:kCMTimeZero];
    [self applyModifiers];
  }
}

#pragma mark - Prop setters

- (void)setPaused:(BOOL)paused
{
  if (paused) {
    [self stopProgressTimer];
    [_player setRate:0.0];
  } else {
    [self startProgressTimer];
    [_player setRate:_rate];
  }

  _paused = paused;
}

- (void)setSeek:(float)seekTime
{
    

  int timeScale = 10000;

  AVPlayerItem *item = _player.currentItem;
  if (item && item.status == AVPlayerItemStatusReadyToPlay) {
    // TODO check loadedTimeRanges

    CMTime cmSeekTime = CMTimeMakeWithSeconds(seekTime, timeScale);
    CMTime current = item.currentTime;
    // TODO figure out a good tolerance level
    CMTime tolerance = CMTimeMake(1000, timeScale);

    if (CMTimeCompare(current, cmSeekTime) != 0) {
      [_player seekToTime:cmSeekTime toleranceBefore:tolerance toleranceAfter:tolerance completionHandler:^(BOOL finished) {
        [_eventDispatcher sendDeviceEventWithName:@"onVideoSeek"
                                            body:@{@"currentTime": [NSNumber numberWithFloat:CMTimeGetSeconds(item.currentTime)],
                                                   @"seekTime": [NSNumber numberWithFloat:seekTime],
                                                   @"target": self.uuid}];
      }];

      _pendingSeek = false;
    }

  } else {
    // TODO: See if this makes sense and if so, actually implement it
    _pendingSeek = true;
    _pendingSeekTime = seekTime;
  }
}

- (void)setRate:(float)rate
{
  _rate = rate;
  [self applyModifiers];
}

- (void)setMuted:(BOOL)muted
{
  _muted = muted;
  [self applyModifiers];
}

- (void)setVolume:(float)volume
{
  _volume = volume;
  [self applyModifiers];
}

- (void)applyModifiers
{
  if (_muted) {
    [_player setVolume:0];
    [_player setMuted:YES];
  } else {
    [_player setVolume:_volume];
    [_player setMuted:NO];
  }

  [self setRepeat:_repeat];
  [self setPaused:_paused];
}

- (void)setRepeat:(BOOL)repeat {
  _repeat = repeat;
}

-(AVPlayer*)getAVPlayer
{
    return _player;
}

#pragma mark - React View Management

- (void)insertReactSubview:(UIView *)view atIndex:(NSInteger)atIndex
{
  RCTLogError(@"video cannot have any subviews");
  return;
}

- (void)removeReactSubview:(UIView *)subview
{
  RCTLogError(@"video cannot have any subviews");
  return;
}

#pragma mark - Lifecycle

-(void)invalidate
{
    [self stopProgressTimer];
}

-(void)dealloc
{
  [_progressUpdateTimer invalidate];
  _prevProgressUpdateTime = nil;

  [_player pause];
  _player = nil;

  [self removePlayerItemObservers];

  _eventDispatcher = nil;
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
