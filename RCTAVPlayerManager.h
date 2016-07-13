#import "RCTBridgeModule.h"
#import "RCTAVPlayer.h"
#import "RCTInvalidating.h"

@interface RCTAVPlayerManager : NSObject <RCTBridgeModule, RCTInvalidating>

+(RCTAVPlayer*)getPlayer:(NSString*)playerUuid;

@end
