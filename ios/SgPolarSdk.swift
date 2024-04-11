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

    bleSdkManager = PolarModuleSDK(self);
  }

  @objc func connectToDevice(_ id: String) {
    self.deviceId = id;
    bleSdkManager?.connectToDevice(id);
  }

  @objc func disconnectFromDevice(_ id: String) {
    bleSdkManager?.disconnectFromDevice(id);
  }

  @objc func startDevicesSearch() {
    bleSdkManager?.startDevicesSearch();
  }

  @objc func stopDevicesSearch() {
    bleSdkManager?.stopDevicesSearch();
  }

  @objc func broadcastToggle() {
    bleSdkManager?.broadcastToggle();
  }

  @objc func getExercises() {
    bleSdkManager?.listH10Exercises();
  }

  @objc func deleteExercise(_ entryId: String) {
    bleSdkManager?.deleteExercise(entryId);
  }

  @objc func checkBle(_ callback: RCTResponseSenderBlock) {
    callback([bleSdkManager?.checkBle() ?? false]);
  }

  @objc func supportedEvents() -> [String] {
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
