package com.sgpolarsdk;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = SgPolarSdkModule.NAME)
public class SgPolarSdkModule extends ReactContextBaseJavaModule {
  public static final String NAME = "SgPolarSdk";

  PolarModuleSDK moduleSDK;
  public String deviceId = "";
  private final boolean bluetoothSupported;

  public SgPolarSdkModule(ReactApplicationContext reactContext) {
    super(reactContext);

    bluetoothSupported = BluetoothAdapter.getDefaultAdapter() != null;
  }

  private PolarModuleSDK manager() {
    if (moduleSDK == null) {
      moduleSDK = new PolarModuleSDK(getReactApplicationContext(), this);
    }
    return moduleSDK;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void connectToDevice(String deviceId) {
    if (!bluetoothSupported) return;
    this.deviceId = deviceId;
    manager().connectToDevice(deviceId);
  }

  @ReactMethod
  public void disconnectFromDevice(String deviceId) {
    if (!bluetoothSupported) return;
    try {
        manager().disconnectFromDevice(deviceId);
    } catch (Exception e) {

    }

    this.deviceId = null;
  }

  @ReactMethod
  public void startDevicesSearch() {
    if (!bluetoothSupported) return;
    manager().scanDevices();
  }

  @ReactMethod
  public void stopDevicesSearch() {
    if (!bluetoothSupported || moduleSDK == null) return;
    moduleSDK.stopScanDevices();
  }

  @ReactMethod
  public void broadcastToggle() {
    if (!bluetoothSupported) return;
    manager().startStreamHr();
  }

  @ReactMethod
  public void getExercises() {
    if (!bluetoothSupported) return;
    manager().listExercises();
  }

  @ReactMethod
  public void checkBle(Callback callback) {
    if (!bluetoothSupported) {
      callback.invoke(false);
      return;
    }
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (!mBluetoothAdapter.isEnabled()) {
      callback.invoke(false);
    } else {
      callback.invoke(true);
    }
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Required for RN's NativeEventEmitter; no-op.
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    // Required for RN's NativeEventEmitter; no-op.
  }
}
