#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(SgPolarSdk, RCTEventEmitter)

RCT_EXTERN_METHOD(connectToDevice:(NSString *)deviceId)
RCT_EXTERN_METHOD(disconnectFromDevice:(NSString *)deviceId)
RCT_EXTERN_METHOD(startDevicesSearch)
RCT_EXTERN_METHOD(stopDevicesSearch)
RCT_EXTERN_METHOD(broadcastToggle)
RCT_EXTERN_METHOD(getExercises)
RCT_EXTERN_METHOD(deleteExercise:(NSString *)entryId)
RCT_EXTERN_METHOD(checkBle: (RCTResponseSenderBlock)callback)

RCT_EXTERN_METHOD(supportedEvents)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
