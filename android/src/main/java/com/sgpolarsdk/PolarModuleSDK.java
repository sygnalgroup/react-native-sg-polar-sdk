package com.sgpolarsdk;

// import android.support.annotation.Nullable;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.HashSet;
        import java.util.Set;
import java.util.UUID;
import java.util.HashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;

import com.facebook.react.bridge.ReadableMap;
import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
        import com.polar.sdk.api.model.PolarDeviceInfo;
        import com.polar.sdk.api.model.PolarExerciseEntry;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;

public class PolarModuleSDK implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private Disposable scanDisposable = null;
    private Disposable hrStreamDisposable = null;
    public Disposable listExercisesDisposable = null;
    public Disposable fetchExerciseDisposable = null;

    public PolarBleApi api;

    // todo make hashmap of disposables with polar device id as key to manage multiple devices
    // (provided the sdk allows it)
    // e.g. :
    // private Map<String, Disposable> polarDevices = new HashMap<String, Disposable>();
    // then we will be able to call startEcgStreaming (and stopEcgStreaming) with a deviceId argument
    // (and not a useless callback)
    // for now we will keep a plugin for a single simultaneously connected sensor.
    // All events are emitted with a "id": deviceId field, so the api won't change much
    // when we add multiple sensor ability

    private ReactApplicationContext ctx;
    public SgPolarSdkModule moduleSDK;

    public PolarModuleSDK(ReactApplicationContext reactContext, SgPolarSdkModule moduleSDK) {
        this.reactContext = reactContext;
        this.ctx = reactContext;
        this.moduleSDK = moduleSDK;

        Set<PolarBleApi.PolarBleSdkFeature> list = new HashSet<PolarBleApi.PolarBleSdkFeature>();
        list.add(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING);
        list.add(PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO);
        list.add(PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO);
        list.add(PolarBleApi.PolarBleSdkFeature.FEATURE_HR);
        list.add(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING);
        list.add(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING);

        api = PolarBleApiDefaultImpl.defaultImplementation(reactContext, list);

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                WritableMap params = Arguments.createMap();
                params.putString("deviceId", moduleSDK.deviceId);
                params.putString("state", powered ? "on" : "off");
                emit(ctx, "bleState", params);
            }

            /* * * * * * * * * * * * DEVICE CONNECTION * * * * * * * * * * * */

            @Override
            public void deviceConnected(PolarDeviceInfo deviceInfo) {
                String id = deviceInfo.getDeviceId();
                WritableMap params = Arguments.createMap();
                params.putString("deviceId", id);
                params.putString("deviceName", deviceInfo.getName());
                params.putString("state", "connected");
                emit(ctx, "connectionState", params);
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo deviceInfo) {
                WritableMap params = Arguments.createMap();
                params.putString("deviceId", deviceInfo.getDeviceId());
                params.putString("deviceName", deviceInfo.getName());
                params.putString("state", "connecting");
                emit(ctx, "connectionState", params);
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo deviceInfo) {
                String id = deviceInfo.getDeviceId();
                WritableMap params = Arguments.createMap();
                params.putString("deviceId", id);
                params.putString("deviceName", deviceInfo.getName());
                params.putString("state", "disconnected");
                emit(ctx, "connectionState", params);
                // set deviceId to null ?
            }

            /* * * * * * * * * * * * * FEATURES READY * * * * * * * * * * * * */

            @Override
            public void hrFeatureReady(String id) {
                WritableMap params = Arguments.createMap();
                params.putString("id", id);
                emit(ctx, "hrFeatureReady", params);
            }

            @Override
            public void disInformationReceived(String id, UUID u, String value) {
                if (u.equals(UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"))) {
                    WritableMap params = Arguments.createMap();
                    params.putString("id", id);
                    params.putString("value", value.trim());
                    emit(ctx, "firmwareVersion", params);
                } else {
                    WritableMap params = Arguments.createMap();
                    params.putString("id", id);
                    params.putString("uuid", u.toString());
                    params.putString("value", value);
                    emit(ctx, "disInformation", params);
                }
            }

            @Override
            public void batteryLevelReceived(String id, int level) {
                WritableMap params = Arguments.createMap();
                params.putString("id", id);
                params.putInt("value", level);
                emit(ctx, "batteryLevel", params);
            }


            @Override
            public void polarFtpFeatureReady(String id) {
                // TODO (wth is polar ftp ? has something to do with exercise entry ?)
            }
        });
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostResume() {
        api.foregroundEntered();
    }

    @Override
    public void onHostDestroy() {
        api.shutDown();
    }

    private void emit(ReactContext reactContext,
                      String eventName,
                      @Nullable WritableMap params) {
        // guard found in some old SO posts
        // (not always necessary but avoids random initialization order bugs)
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    public void connectToDevice(String id) {
        try {
            api.connectToDevice(id);
        } catch (Exception e) {

        }
    }

    public void startStreamHr() {
        boolean isDisposed = hrStreamDisposable != null ? hrStreamDisposable.isDisposed() : true;

        if (isDisposed) {
            hrStreamDisposable = api.startHrStreaming(moduleSDK.deviceId)
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(polarHrData -> {
                        WritableMap params = Arguments.createMap();
                        params.putString("state", "on");
                        params.putString("hr", "" + polarHrData.getSamples().get(polarHrData.getSamples().size() - 1).getHr());
                        emit(reactContext, "hrStatus", params);
                    }, error -> {
                        WritableMap params = Arguments.createMap();
                        params.putString("state", "off");
                        emit(reactContext, "hrStatus", params);
                    }, new Action() {
                        @Override
                        public void run() throws Throwable {
                            WritableMap params = Arguments.createMap();
                            params.putString("state", "off");
                            emit(reactContext, "hrStatus", params);
                        }
                    });
        } else {
            hrStreamDisposable.dispose();
        }
    }

    public void disconnectFromDevice(String id) {
        // this.deviceId = id;
        WritableMap params = Arguments.createMap();
        params.putString("id", id);
        params.putString("state", "disconnecting");
        emit(reactContext, "connectionState", params);
        try {
            // api.disconnectFromDevice(this.deviceId);
            api.disconnectFromDevice(id);
        } catch (Exception e) {
            params.putString("state", "disconnected");
            emit(reactContext, "connectionState", params);
        }
    }

    public void scanDevices() {
        boolean isDisposed = scanDisposable != null ? scanDisposable.isDisposed() : true;

        if (isDisposed) {
            WritableMap p = Arguments.createMap();
            p.putString("state", "inProgress");
            emit(reactContext, "searchDeviceStatus", p);

            scanDisposable = api.searchForDevice()
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(polarDeviceInfo -> {
                        WritableMap params = Arguments.createMap();
                        params.putString("deviceId", polarDeviceInfo.getDeviceId());
                        params.putString("deviceName", polarDeviceInfo.getName());
                        emit(reactContext, "searchDeviceItem", params);
                    }, error -> {
                        WritableMap params = Arguments.createMap();
                        params.putString("state", "complete");
                        emit(reactContext, "searchDeviceStatus", params);
                    }, new Action() {
                        @Override
                        public void run() throws Throwable {
                            WritableMap params = Arguments.createMap();
                            params.putString("state", "complete");
                            emit(reactContext, "searchDeviceStatus", params);
                        }
                    });
        } else {
            scanDisposable.dispose();
        }
    }

    public void stopScanDevices() {
        WritableMap params = Arguments.createMap();
        params.putString("state", "complete");
        emit(reactContext, "searchDeviceStatus", params);
        scanDisposable.dispose();
    }

    public void listExercises() {
        boolean isDisposed = listExercisesDisposable != null ? listExercisesDisposable.isDisposed() : true;

        if (isDisposed) {
            Log.d("LIST START", "");
            listExercisesDisposable = api.listExercises(moduleSDK.deviceId)
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(polarExerciseEntry -> {
                       fetchExercise(polarExerciseEntry);
                    }, error -> {
                        WritableMap params = Arguments.createMap();
                        params.putString("error", "Error to list exercises, device in use.");
                        emit(reactContext, "listExerciseComplete", params);
                    }, new Action() {
                        @Override
                        public void run() throws Throwable {
                            WritableMap params = Arguments.createMap();
                            emit(reactContext, "listExerciseComplete", params);
                        }
                    });
        } else {
            listExercisesDisposable.dispose();
        }
    }

    public void deleteExercise(String entryId) {
        boolean isDisposed = listExercisesDisposable != null ? listExercisesDisposable.isDisposed() : true;

        if (isDisposed) {
            listExercisesDisposable = api.listExercises(moduleSDK.deviceId)
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(polarExerciseEntry -> {
                        if (polarExerciseEntry.getIdentifier() == entryId) {

                            api.removeExercise(moduleSDK.deviceId, polarExerciseEntry)
                                    .observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {
                                        WritableMap params = Arguments.createMap();
                                        params.putString("id", polarExerciseEntry.getIdentifier());
                                        params.putBoolean("_destroy", true);
                                        emit(reactContext, "listExerciseItem", params);

                                    }, error -> {});
                        }
                    }, error -> {

                    }, new Action() {
                        @Override
                        public void run() throws Throwable {
                        }
                    });
        } else {
            listExercisesDisposable.dispose();
        }
    }

    public void fetchExercise(PolarExerciseEntry polarExerciseEntry) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

            WritableMap params = Arguments.createMap();

            boolean isDisposed = fetchExerciseDisposable != null ? fetchExerciseDisposable.isDisposed() : true;

            if (isDisposed) {
                fetchExerciseDisposable = api.fetchExercise(moduleSDK.deviceId, polarExerciseEntry)
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(polarExerciseData -> {
                            WritableArray array = Arguments.fromList(polarExerciseData.getHrSamples());

                            params.putString("id", polarExerciseEntry.getIdentifier());
                            params.putArray("data", array);
                            params.putString("date", simpleDateFormat.format(polarExerciseEntry.getDate()));
                            emit(reactContext, "listExerciseItem", params);
                        }, error -> {});
            } else {
                fetchExerciseDisposable.dispose();
            }
        } catch (Exception e) {
            Log.d("LIST FETCH START", e.getMessage());
        }
    }
}
