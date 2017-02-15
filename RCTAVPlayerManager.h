#import <React/RCTBridgeModule.h>
#import "RCTAVPlayer.h"
#import <React/RCTInvalidating.h>

@interface RCTAVPlayerManager : NSObject <RCTBridgeModule, RCTInvalidating>

+(RCTAVPlayer*)getPlayer:(NSString*)playerUuid;

@end
