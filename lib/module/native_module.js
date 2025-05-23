"use strict";

import { NativeModules, Platform } from 'react-native';
const LINKING_ERROR = `The package 'react-native-usb-serialport-for-android' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo managed workflow\n';
const UsbSerialportForAndroid = NativeModules.UsbSerialportForAndroid ? NativeModules.UsbSerialportForAndroid : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
export default UsbSerialportForAndroid;
//# sourceMappingURL=native_module.js.map