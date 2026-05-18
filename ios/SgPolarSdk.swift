import Foundation
import PolarBleSdk
import RxSwift
import CoreBluetooth

@objc(SgPolarSdk)
class SgPolarSdk: RCTEventEmitter {
  @Published var bleSdkManager: PolarModuleSDK? = nil
  @Published var deviceId = ""

  override init() {
    super.init()
  }

  private func manager() -> PolarModuleSDK {
    if bleSdkManager == nil {
      bleSdkManager = PolarModuleSDK(self)
    }
    return bleSdkManager!
  }

  @objc func connectToDevice(_ id: String) {
    self.deviceId = id;
    manager().connectToDevice(id);
  }

  @objc func disconnectFromDevice(_ id: String) {
    manager().disconnectFromDevice(id);
  }

  @objc func startDevicesSearch() {
    manager().startDevicesSearch();
  }

  @objc func stopDevicesSearch() {
    bleSdkManager?.stopDevicesSearch();
  }

  @objc func broadcastToggle() {
    manager().broadcastToggle();
  }

  @objc func getExercises() {
    manager().listH10Exercises();
  }

  @objc func deleteExercise(_ entryId: String) {
    manager().deleteExercise(entryId);
  }

  @objc func checkBle(_ callback: RCTResponseSenderBlock) {
    callback([manager().checkBle()]);
  }

  @objc override func supportedEvents() -> [String] {
  return [
    "connectionState",
    "listExerciseItem",
    "listExerciseComplete",
    "searchDeviceItem",
    "searchDeviceStatus",
    "bleState",
    "hrStatus"
  ]
}
}
