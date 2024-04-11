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

  public SgPolarSdkModule(ReactApplicationContext reactContext) {
    super(reactContext);

    moduleSDK = new PolarModuleSDK(reactContext, this);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void connectToDevice(String deviceId) {
    this.deviceId = deviceId;
    moduleSDK.connectToDevice(deviceId);
  }

  @ReactMethod
  public void disconnectFromDevice(String deviceId) {
    try {
        moduleSDK.disconnectFromDevice(deviceId);
    } catch (Exception e) {

    }

    this.deviceId = null;
  }

  @ReactMethod
  public void startDevicesSearch() {
    moduleSDK.scanDevices();
  }

  @ReactMethod
  public void stopDevicesSearch() {
    moduleSDK.stopScanDevices();
  }

  @ReactMethod
  public void broadcastToggle() {
    moduleSDK.startStreamHr();
  }

  @ReactMethod
  public void getExercises() {
    moduleSDK.listExercises();
  }

  @ReactMethod
  public void checkBle(Callback callback) {
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (mBluetoothAdapter == null) {
      // Device does not support Bluetooth
      callback.invoke(false);
    } else if (!mBluetoothAdapter.isEnabled()) {
      // Bluetooth is not enabled :)
      callback.invoke(false);
    } else {
      // Bluetooth is enabled
      callback.invoke(true);
    }
  }
}
