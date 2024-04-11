import Foundation
import PolarBleSdk
import RxSwift
import CoreBluetooth

class PolarModuleSDK: ObservableObject, PolarBleApiLogger, PolarBleApiDeviceInfoObserver, PolarBleApiPowerStateObserver, PolarBleApiDeviceFeaturesObserver, PolarBleApiObserver {
  
  // NOTICE this example utilises all available features
  private var api = PolarBleApiDefaultImpl.polarImplementation(DispatchQueue.main,
                                                               features: [PolarBleSdkFeature.feature_hr,
                                                                          PolarBleSdkFeature.feature_polar_sdk_mode,
                                                                          PolarBleSdkFeature.feature_battery_info,
                                                                          PolarBleSdkFeature.feature_device_info,
                                                                          PolarBleSdkFeature.feature_polar_online_streaming,
                                                                          PolarBleSdkFeature.feature_polar_offline_recording,
                                                                          PolarBleSdkFeature.feature_polar_device_time_setup,
                                                                          PolarBleSdkFeature.feature_polar_h10_exercise_recording]
  )

  @Published var offlineRecordingFeature = OfflineRecordingFeature()
  @Published var offlineRecordingSettings: RecordingSettings? = nil
  @Published var offlineRecordingEntries: OfflineRecordingEntries = OfflineRecordingEntries()
  @Published var offlineRecordingData: OfflineRecordingData = OfflineRecordingData()
  @Published var deviceSearch: DeviceSearch = DeviceSearch()

  private let disposeBag = DisposeBag()
  private var broadcastDisposable: Disposable?

  @Published var isBroadcastListenOn: Bool = false

  private var h10ExerciseEntry: PolarExerciseEntry?

  private var sdkModule: SgPolarSdk?
  private var searchDevicesTask: Task<Void, Never>? = nil

  init(_ sdkModule: SgPolarSdk) {
    api.polarFilter(true)
    api.observer = self
    api.deviceFeaturesObserver = self
    api.powerStateObserver = self
    api.deviceInfoObserver = self
    api.logger = self

    self.sdkModule = sdkModule;
  }

  @objc func connectToDevice(_ id: String) {
    do {
      try api.connectToDevice(id)
    } catch let err {
      NSLog("Failed to connect to \(id). Reason \(err)")
    }
  }

  @objc func checkBle() -> Bool {
    return api.isBlePowered;
  }

  @objc func disconnectFromDevice(_ id: String) {
    do {
      try api.disconnectFromDevice(id)
    } catch let err {
      NSLog("Failed to disconnect from \(id). Reason \(err)")
    }
  }

  func broadcastToggle() {
    if isBroadcastListenOn == false {
      isBroadcastListenOn = true
      broadcastDisposable = api.startListenForPolarHrBroadcasts(nil)
        .observe(on: MainScheduler.instance)
        .subscribe{ e in
          switch e {
            case .completed:
              self.isBroadcastListenOn = false
              NSLog("Broadcast listener completed")
              self.sdkModule?.sendEvent(withName: "hrStatus", body: [ "state": "off", "hr": 0 ] as [String : Any])
            case .error(let err):
              self.isBroadcastListenOn = false
              NSLog("Broadcast listener failed. Reason: \(err)")
            case .next(let broadcast):
              self.sdkModule?.sendEvent(withName: "hrStatus", body: [ "state": "on", "hr": broadcast.hr ] as [String : Any])
              NSLog("HR BROADCAST \(broadcast.deviceInfo.name) HR:\(broadcast.hr) Batt: \(broadcast.batteryStatus)")
          }
        }
    } else {
      isBroadcastListenOn = false
      self.sdkModule?.sendEvent(withName: "hrStatus", body: [ "state": "off", "hr": 0 ] as [String : Any])
      broadcastDisposable?.dispose()
    }
  }

  func listH10Exercises() {
    let device = sdkModule?.deviceId

    h10ExerciseEntry = nil

    api.fetchStoredExerciseList(device ?? "")
      .observe(on: MainScheduler.instance)
      .subscribe{ [self] e in
        switch e {
          case .completed:
            NSLog("list exercises completed")
            self.sdkModule?.sendEvent(withName: "listExerciseComplete", body: [])
          case .error(let err):
            NSLog("failed to list exercises: \(err)")
            self.sdkModule?.sendEvent(withName: "listExerciseComplete", body: [ "error": "Error to list exercises, device in use." ])
          case .next(let polarExerciseEntry):
            Task {
              await self.sendExerciseEvent(polarExerciseEntry)
            }
            NSLog("entry: \(polarExerciseEntry.date.description) path: \(polarExerciseEntry.path) id: \(polarExerciseEntry.entryId)");
            self.h10ExerciseEntry = polarExerciseEntry
        }
      }.disposed(by: disposeBag)
  }

  func sendExerciseEvent(_ e: PolarExerciseEntry) async -> Void {
    let records = await self.h10ReadExercise(e)
    let dateFormatter = DateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    dateFormatter.timeZone = NSTimeZone(name: "UTC")! as? TimeZone
    let formated = dateFormatter.string(from: e.date)
    self.sdkModule?.sendEvent(withName: "listExerciseItem", body: [ "id": e.entryId, "date": formated, "data": records ])
  }

  func h10ReadExercise(_ e: PolarExerciseEntry) async -> Array<UInt32> {
    let device = sdkModule?.deviceId ?? ""

    do {

      let data:PolarExerciseData = try await api.fetchExercise(device, entry: e).value
      NSLog("exercise data count: \(data.samples.count) samples: \(data.samples)")

      return data.samples
    } catch let err {
     return []
    }

    return []
  }

  func deviceConnecting(_ polarDeviceInfo: PolarBleSdk.PolarDeviceInfo) {
    print("DEVICE CONNECTING:")
    Task { @MainActor in
      sdkModule?.sendEvent(withName: "connectionState", body: [ "state": "connecting" ])
    }
  }

  func deviceConnected(_ polarDeviceInfo: PolarBleSdk.PolarDeviceInfo) {
    print("DEVICE CONNECTED:")

    Task { @MainActor in
      sdkModule?.sendEvent(withName: "connectionState", body: [ 
        "state": "connected", "deviceId": polarDeviceInfo.deviceId, "deviceName": polarDeviceInfo.name
      ])
    }
  }
  
  func deleteExercise(_ entryId: String) {
    let device = sdkModule?.deviceId
    
    api.fetchStoredExerciseList(device ?? "")
      .observe(on: MainScheduler.instance)
      .subscribe{ [self] e in
        switch e {
          case .completed:
            NSLog("list exercises completed")
          case .error(let err):
            NSLog("failed to list exercises: \(err)")
          case .next(let polarExerciseEntry):
            Task {
              if (polarExerciseEntry.entryId == entryId) {
                api.removeExercise(sdkModule?.deviceId ?? "", entry: polarExerciseEntry)
                  .observe(on: MainScheduler.instance)
                  .subscribe{ [self] e in
                    switch e {
                      case .completed:
                        NSLog("remove exercises completed")
                        self.sdkModule?.sendEvent(withName: "listExerciseItem", body: [ "id": polarExerciseEntry.entryId, "_destroy": true ] as [String : Any])
                      case .error(let err):
                        NSLog("failed to remove exercises: \(err)")
                    }
                  }.disposed(by: disposeBag)
              }
            }
        }
      }.disposed(by: disposeBag)
  }

  func deviceDisconnected(_ polarDeviceInfo: PolarBleSdk.PolarDeviceInfo, pairingError: Bool) {
    Task { @MainActor in
      sdkModule?.sendEvent(withName: "connectionState", body: [ "state": "disconnected" ])

      self.offlineRecordingFeature = OfflineRecordingFeature()
    }
  }

  func startDevicesSearch() {
    searchDevicesTask = Task {
      await searchDevicesAsync()
    }
  }

  func stopDevicesSearch() {
    searchDevicesTask?.cancel()
    searchDevicesTask = nil
    Task { @MainActor in
      self.deviceSearch.isSearching = DeviceSearchState.success
    }
  }

  private func searchDevicesAsync() async {
    Task { @MainActor in
      self.deviceSearch.foundDevices.removeAll()
      self.deviceSearch.isSearching = DeviceSearchState.inProgress
      self.sdkModule?.sendEvent(withName: "searchDeviceStatus", body: [ "state": "inProgress" ])
    }

    do {
      for try await value in api.searchForDevice().values {
        Task { @MainActor in
          self.sdkModule?.sendEvent(withName: "searchDeviceItem", body: [ "deviceId": value.deviceId, "deviceName": value.name ])
          self.deviceSearch.foundDevices.append(value)
        }
      }
      Task { @MainActor in
        self.deviceSearch.isSearching = DeviceSearchState.success
        self.sdkModule?.sendEvent(withName: "searchDeviceStatus", body: [ "state": "complete" ])
      }
    } catch let err {
      let deviceSearchFailed = "device search failed: \(err)"
      NSLog(deviceSearchFailed)
      Task { @MainActor in
        self.deviceSearch.isSearching = DeviceSearchState.failed(error: deviceSearchFailed)
      }
    }
  }

  func hrFeatureReady(_ identifier: String) {

  }

  func ftpFeatureReady(_ identifier: String) {

  }

  func streamingFeaturesReady(_ identifier: String, streamingFeatures: Set<PolarBleSdk.PolarDeviceDataType>) {

  }

  func bleSdkFeatureReady(_ identifier: String, feature: PolarBleSdk.PolarBleSdkFeature) {

  }

  func blePowerOn() {
    self.sdkModule?.sendEvent(withName: "bleState", body: [ "state": "on" ])
  }

  func blePowerOff() {
    self.sdkModule?.sendEvent(withName: "bleState", body: [ "state": "off" ])
  }

  func batteryLevelReceived(_ identifier: String, batteryLevel: UInt) {

  }

  func disInformationReceived(_ identifier: String, uuid: CBUUID, value: String) {

  }

  func message(_ str: String) {

  }
}
