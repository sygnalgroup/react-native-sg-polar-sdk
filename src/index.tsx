import { NativeModules, Platform, NativeEventEmitter } from 'react-native';
import type { EmitterSubscription } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-sg-polar-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const SgPolarSdk = NativeModules.SgPolarSdk
  ? NativeModules.SgPolarSdk
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const polarEmmiter = new NativeEventEmitter(SgPolarSdk);

type PolarEvent =
  | 'connectionState'
  | 'listExerciseItem'
  | 'listExerciseComplete'
  | 'searchDeviceItem'
  | 'searchDeviceStatus'
  | 'bleState'
  | 'hrStatus';

export const connectToDevice: (id: string) => void = SgPolarSdk.connectToDevice;

export const disconnectFromDevice: (id: string) => void =
  SgPolarSdk.disconnectFromDevice;

export const startDevicesSearch: () => void = SgPolarSdk.startDevicesSearch;

export const stopDevicesSearch: () => void = SgPolarSdk.stopDevicesSearch;

export const broadcastToggle: () => void = SgPolarSdk.broadcastToggle;

export const getExercises: () => void = SgPolarSdk.listH10Exercises;

export const deleteExercise: (entryId: string) => void =
  SgPolarSdk.deleteExercise;

export const checkBle: (callback: (ble: boolean) => {}) => void =
  SgPolarSdk.checkBle;

export const addListener: (
  eventType: PolarEvent,
  listener: (event: any) => void,
  context?: Object
) => EmitterSubscription = polarEmmiter.addListener;
