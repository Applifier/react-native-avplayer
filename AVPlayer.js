/**
 *
 * @providesModule AVPlayer
 *
*/

'use strict';

var NativeModules = require('react-native').NativeModules;
var NativeVideo = NativeModules.AVPlayer;
var React = require('react-native');
var EventEmitter = require('eventemitter3');
var {
  DeviceEventEmitter,
  Platform
} = React;

function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

class AVPlayer extends EventEmitter {

  constructor() {
    super();
    this.uuid = guid();
    var self = this;
    NativeVideo.createVideoPlayer(this.uuid);
    this._loadStartListener = DeviceEventEmitter.addListener('onVideoLoadStart', (body) => {
      if (body.target === this.uuid) {
        this.emit('loadStart', body);
      }
    });
    this._loadListener = DeviceEventEmitter.addListener('onVideoLoad', (body) => {
      if (body.target === this.uuid) {
        this._duration = body.duration;
        this.emit('load', body);
      }
    });
    this._errorListener = DeviceEventEmitter.addListener('onVideoError', (body) => {
      if (body.target === this.uuid || this._errorEventMatchesOnAndroid(body)) {
        this.emit('error', body);
      }
    });
    this._progressListener = DeviceEventEmitter.addListener('onVideoProgress', (body) => {
      if (body.target === this.uuid || this._progressEventMatchesOnAndroid(body)) {
        this._currentTime = body.currentTime;
        this.emit('progress', body);
      }
    });
    this._seekListener = DeviceEventEmitter.addListener('onVideoSeek', (body) => {
      if (body.target === this.uuid || this._seekEventMatchesOnAndroid(body)) {
        this._currentTime = body.currentTime;
        this.emit('seek', body);
      }
    });
    this._endListener = DeviceEventEmitter.addListener('onVideoEnd', (body) => {
      if (body.target === this.uuid) {
        this.emit('end', body);
      }
    });
    this.rate = 1;
    this.volume = 1;
    this.repeat = true;
    this.muted = false,
    this._source = null;
  }
  // seek event is missing uuid on older versions on Android
  _seekEventMatchesOnAndroid (body) {
    return Platform.OS === 'android' && this._seekTime === body.seekTime;
  }
  // progress event is missing uuid on older versions on Android
  _progressEventMatchesOnAndroid (body) {
    return Platform.OS === 'android' && this._duration === body.playableDuration;
  }
  // error event is missing uuid on older versions on Android
  _errorEventMatchesOnAndroid (body) {
    return Platform.OS === 'android';
  }
  set source(source) {
    var uri = source.uri;
    if (uri && uri.match(/^\//)) {
      uri = 'file://' + uri;
    }
    var isNetwork = !!(uri && uri.match(/^https?:/));
    var isAsset = !!(uri && uri.match(/^(assets-library|file):/));
    var src = {
      uri: uri,
      isNetwork,
      isAsset,
      type: source.type || 'mp4'
    };
    NativeVideo.setSource(this.uuid, src, function(err) {});
    this._source = source;
  }
  get source() {
    return this._source;
  }
  set repeat(repeat) {
    NativeVideo.setRepeat(this.uuid, repeat, function(err) {});
    this._repeat = repeat;
  }
  get repeat() {
    return this._repeat;
  }
  set muted(muted) {
    NativeVideo.setMuted(this.uuid, muted, function(err) {});
    this._muted = muted;
  }
  get muted() {
    return this._muted;
  }
  set volume(volume) {
    NativeVideo.setVolume(this.uuid, volume, function(err) {});
    this._volume = volume;
  }
  get volume() {
    return this._volume;
  }
  set rate(rate) {
    NativeVideo.setRate(this.uuid, rate, function(err) {});
    this._rate = rate;
  }
  get rate() {
    return this._rate;
  }
  set currentTime(seek) {
    this._seekTime = seek;
    NativeVideo.setSeek(this.uuid, seek, function(err) {});
  }
  get currentTime() {
    return this._currentTime;
  }
  release() {
    NativeVideo.removePlayer(this.uuid, function(err) {});
    this._loadStartListener.remove();
    this._loadListener.remove();
    this._errorListener.remove();
    this._progressListener.remove();
    this._seekListener.remove();
    this._endListener.remove();
  }
}

module.exports = AVPlayer;
