"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Codes = void 0;
Object.defineProperty(exports, "Device", {
  enumerable: true,
  get: function () {
    return _native_module.Device;
  }
});
Object.defineProperty(exports, "EventData", {
  enumerable: true,
  get: function () {
    return _usb_serial.EventData;
  }
});
Object.defineProperty(exports, "Listener", {
  enumerable: true,
  get: function () {
    return _usb_serial.Listener;
  }
});
exports.Parity = void 0;
Object.defineProperty(exports, "UsbSerial", {
  enumerable: true,
  get: function () {
    return _usb_serial.default;
  }
});
exports.UsbSerialManager = void 0;
var _reactNative = require("react-native");
var _native_module = _interopRequireWildcard(require("./native_module"));
var _usb_serial = _interopRequireWildcard(require("./usb_serial"));
function _getRequireWildcardCache(e) { if ("function" != typeof WeakMap) return null; var r = new WeakMap(), t = new WeakMap(); return (_getRequireWildcardCache = function (e) { return e ? t : r; })(e); }
function _interopRequireWildcard(e, r) { if (!r && e && e.__esModule) return e; if (null === e || "object" != typeof e && "function" != typeof e) return { default: e }; var t = _getRequireWildcardCache(r); if (t && t.has(e)) return t.get(e); var n = { __proto__: null }, a = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var u in e) if ("default" !== u && {}.hasOwnProperty.call(e, u)) { var i = a ? Object.getOwnPropertyDescriptor(e, u) : null; i && (i.get || i.set) ? Object.defineProperty(n, u, i) : n[u] = e[u]; } return n.default = e, t && t.set(e, n), n; }
const {
  CODE_DEVICE_NOT_FOND,
  CODE_DRIVER_NOT_FOND,
  CODE_NOT_ENOUGH_PORTS,
  CODE_PERMISSION_DENIED,
  CODE_OPEN_FAILED,
  CODE_DEVICE_NOT_OPEN,
  CODE_SEND_FAILED,
  CODE_DEVICE_NOT_OPEN_OR_CLOSED
} = _reactNative.NativeModules.UsbSerialportForAndroid.getConstants();
const Codes = exports.Codes = {
  DEVICE_NOT_FOND: CODE_DEVICE_NOT_FOND,
  DRIVER_NOT_FOND: CODE_DRIVER_NOT_FOND,
  NOT_ENOUGH_PORTS: CODE_NOT_ENOUGH_PORTS,
  PERMISSION_DENIED: CODE_PERMISSION_DENIED,
  OPEN_FAILED: CODE_OPEN_FAILED,
  DEVICE_NOT_OPEN: CODE_DEVICE_NOT_OPEN,
  SEND_FAILED: CODE_SEND_FAILED,
  DEVICE_NOT_OPEN_OR_CLOSED: CODE_DEVICE_NOT_OPEN_OR_CLOSED
};
const eventEmitter = new _reactNative.NativeEventEmitter(_reactNative.NativeModules.UsbSerialportForAndroid);
let Parity = exports.Parity = /*#__PURE__*/function (Parity) {
  Parity[Parity["None"] = 0] = "None";
  Parity[Parity["Odd"] = 1] = "Odd";
  Parity[Parity["Even"] = 2] = "Even";
  Parity[Parity["Mark"] = 3] = "Mark";
  Parity[Parity["Space"] = 4] = "Space";
  return Parity;
}({});
const defaultManager = {
  list() {
    return _native_module.default.list();
  },
  async tryRequestPermission(deviceId) {
    const result = await _native_module.default.tryRequestPermission(deviceId);
    return result === 1;
  },
  hasPermission(deviceId) {
    return _native_module.default.hasPermission(deviceId);
  },
  async open(deviceId, options) {
    await _native_module.default.open(deviceId, options.baudRate, options.dataBits, options.stopBits, options.parity);
    return new _usb_serial.default(deviceId, eventEmitter);
  }
};
const UsbSerialManager = exports.UsbSerialManager = _reactNative.Platform.OS === 'android' ? defaultManager : new Proxy({}, {
  get() {
    return () => {
      throw new Error(`Not support ${_reactNative.Platform.OS}`);
    };
  }
});
//# sourceMappingURL=index.js.map