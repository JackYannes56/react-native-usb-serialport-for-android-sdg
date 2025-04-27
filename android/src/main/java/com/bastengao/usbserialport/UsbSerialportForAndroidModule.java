package com.bastengao.usbserialport;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = UsbSerialportForAndroidModule.NAME)
public class UsbSerialportForAndroidModule extends ReactContextBaseJavaModule implements EventSender {
    public static final String NAME = "UsbSerialportForAndroid";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB";

    public static final String CODE_DEVICE_NOT_FOND = "device_not_found";
    public static final String CODE_DRIVER_NOT_FOND = "driver_not_found";
    public static final String CODE_NOT_ENOUGH_PORTS = "not_enough_ports";
    public static final String CODE_PERMISSION_DENIED = "permission_denied";
    public static final String CODE_OPEN_FAILED = "open_failed";
    public static final String CODE_DEVICE_NOT_OPEN = "device_not_open";
    public static final String CODE_SEND_FAILED = "send_failed";
    public static final String CODE_READ_FAILED = "read_failed";
    public static final String CODE_DEVICE_NOT_OPEN_OR_CLOSED = "device_not_open_or_closed";

    private final ReactApplicationContext reactContext;
    private final Map<Integer, UsbSerialPortWrapper> usbSerialPorts = new HashMap<Integer, UsbSerialPortWrapper>();

    private void registerUsbReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Register for Android 12+
                reactContext.registerReceiver(usbReceiver, filter, null, null);
            } else {
                // Register for older versions
                reactContext.registerReceiver(usbReceiver, filter);
            }
            Log.d("usbserialport", "USB receiver registered successfully.");
        } catch (Exception e) {
            Log.e("usbserialport", "Error registering USB receiver: " + e.getMessage());
        }
    }
    
    
    
    

    public UsbSerialportForAndroidModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        // Register the USB receiver when the module is initialized
        registerUsbReceiver();
    }

    

    @Override
public void onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy();
    try {
        reactContext.unregisterReceiver(usbReceiver);
        Log.d("usbserialport", "USB receiver unregistered successfully.");
    } catch (IllegalArgumentException e) {
        Log.w("usbserialport", "USB receiver was not registered or already unregistered.");
    }
}




    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("CODE_DEVICE_NOT_FOND", CODE_DEVICE_NOT_FOND);
        constants.put("CODE_DRIVER_NOT_FOND", CODE_DRIVER_NOT_FOND);
        constants.put("CODE_NOT_ENOUGH_PORTS", CODE_NOT_ENOUGH_PORTS);
        constants.put("CODE_PERMISSION_DENIED", CODE_PERMISSION_DENIED);
        constants.put("CODE_OPEN_FAILED", CODE_OPEN_FAILED);
        constants.put("CODE_DEVICE_NOT_OPEN", CODE_DEVICE_NOT_OPEN);
        constants.put("CODE_SEND_FAILED", CODE_SEND_FAILED);
        constants.put("CODE_READ_FAILED", CODE_READ_FAILED);
        constants.put("CODE_DEVICE_NOT_OPEN_OR_CLOSED", CODE_DEVICE_NOT_OPEN_OR_CLOSED);
        return constants;
    }

    @ReactMethod
    public void list(Promise promise) {
        WritableArray devices = Arguments.createArray();
        UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            WritableMap d = Arguments.createMap();
            d.putInt("deviceId", device.getDeviceId());
            d.putInt("vendorId", device.getVendorId());
            d.putInt("productId", device.getProductId());
            devices.pushMap(d);
        }
        promise.resolve(devices);
    }

    @ReactMethod
public void tryRequestPermission(int deviceId, Promise promise) {
    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    UsbDevice device = findDevice(deviceId);

    if (device == null) {
        promise.reject(CODE_DEVICE_NOT_FOND, "Device not found");
        return;
    }

    if (usbManager.hasPermission(device)) {
        Log.d("usbserialport", "Permission already granted for device: " + deviceId);
        promise.resolve(1);
        return;
    }

    if (getCurrentActivity() == null) {
        promise.reject("activity_null", "Current activity is null. Cannot request permission.");
        return;
    }

    BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                try {
                    if (getCurrentActivity() != null) {
                        getCurrentActivity().unregisterReceiver(this); // Unregister the receiver
                    }
                } catch (IllegalArgumentException e) {
                    Log.w("usbserialport", "Receiver already unregistered or activity is null.");
                }

                if (permissionGranted) {
                    Log.d("usbserialport", "USB permission granted for device: " + deviceId);
                    promise.resolve(1);
                } else {
                    Log.e("usbserialport", "USB permission denied for device: " + deviceId);
                    promise.reject(CODE_PERMISSION_DENIED, "Permission denied");
                }
            }
        }
    };

    IntentFilter filter = new IntentFilter(INTENT_ACTION_GRANT_USB);

    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getCurrentActivity().registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getCurrentActivity().registerReceiver(usbPermissionReceiver, filter);
        }
    } catch (Exception e) {
        Log.e("usbserialport", "Error registering permission receiver: " + e.getMessage());
        promise.reject("receiver_error", "Failed to register permission receiver");
        return;
    }

    PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(
        getCurrentActivity(),
        0,
        new Intent(INTENT_ACTION_GRANT_USB),
        PendingIntent.FLAG_IMMUTABLE
    );

    try {
        usbManager.requestPermission(device, usbPermissionIntent);
        Log.d("usbserialport", "Permission request sent for device: " + deviceId);
    } catch (Exception e) {
        Log.e("usbserialport", "Error requesting USB permission: " + e.getMessage());
        promise.reject("permission_request_error", "Failed to request USB permission");
    }
}


    @ReactMethod
    public void hasPermission(int deviceId, Promise promise) {
        UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
        UsbDevice device = findDevice(deviceId);
        if (device == null) {
            promise.reject(CODE_DEVICE_NOT_FOND, "device not found");
            return;
        }

        promise.resolve(usbManager.hasPermission(device));
        return;
    }

    @ReactMethod
    public void open(int deviceId, int baudRate, int dataBits, int stopBits, int parity, Promise promise) {
        UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
        if (wrapper != null) {
            promise.resolve(deviceId);
            return;
        }

        UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
        UsbDevice device = findDevice(deviceId);
        if (device == null) {
            promise.reject(CODE_DEVICE_NOT_FOND, "device not found");
            return;
        }

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if (driver == null) {
            promise.reject(CODE_DRIVER_NOT_FOND, "no driver for device");
            return;
        }
        if (driver.getPorts().size() < 0) {
            promise.reject(CODE_NOT_ENOUGH_PORTS, "not enough ports at device");
            return;
        }

        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
        if(connection == null) {
            if (!usbManager.hasPermission(driver.getDevice())) {
                promise.reject(CODE_PERMISSION_DENIED, "connection failed: permission denied");
            } else {
                promise.reject(CODE_OPEN_FAILED, "connection failed: open failed");
            }
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(baudRate, dataBits, stopBits, parity);
        } catch (IOException e) {
            try {
                 port.close();
            } catch (IOException ignored) {}
            promise.reject(CODE_OPEN_FAILED, "connection failed", e);
            return;
        }

        wrapper = new UsbSerialPortWrapper(deviceId, port, this);
        usbSerialPorts.put(deviceId, wrapper);
        promise.resolve(deviceId);
    }

    @ReactMethod
    public void send(int deviceId, String hexStr, Promise promise) {
        UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
        if (wrapper == null) {
            promise.reject(CODE_DEVICE_NOT_OPEN, "device not open");
            return;
        }

        byte[] data = hexStringToByteArray(hexStr);
        wrapper.send(data, exception -> {
            if (exception == null) {
                promise.resolve(null);
            } else {
                promise.reject(CODE_SEND_FAILED, "send failed", exception);
            }
        });
    }

    @ReactMethod
    public void sendWithResponse(int deviceId, String hexStr, int bytes, Promise promise) {
        UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
        if (wrapper == null) {
            promise.reject(CODE_DEVICE_NOT_OPEN, "device not open");
            return;
        }

        byte[] data = hexStringToByteArray(hexStr);
        wrapper.send(data, exception -> {
            if (exception == null) {
                wrapper.read(bytes, promise);
            } else {
                promise.reject(CODE_SEND_FAILED, "send failed", exception);
            }
        });
    }

    // NEW READ FUNCTION
    @ReactMethod
    public void readData(int deviceId, int bytesToRead, Promise promise) {
        UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
        if (wrapper == null) {
            promise.reject(CODE_DEVICE_NOT_OPEN, "Device not open");
            return;
        }

        try {
            // Read the specified number of bytes
            wrapper.read(bytesToRead, promise);
        } catch (Exception e) {
            promise.reject(CODE_READ_FAILED, "Failed to read data", e);
        }
    }


    @ReactMethod
    public void close(int deviceId, Promise promise) {
        UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
        if (wrapper == null) {
            promise.reject(CODE_DEVICE_NOT_OPEN_OR_CLOSED, "serial port not open or closed");
            return;
        }

        wrapper.close();
        usbSerialPorts.remove(deviceId);
        promise.resolve(null);
    }

    public void sendEvent(final String eventName, final WritableMap event) {
        reactContext.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, event);
            }
        });
    }

    private UsbDevice findDevice(int deviceId) {
        UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getDeviceId() == deviceId) {
                return device;
            }
        }

        return null;
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s.length() % 2 == 1) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                handleUsbDeviceAttached(device);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                handleUsbDeviceDetached(device);
            } else {
                Log.w("usbserialport", "Unknown USB action received: " + action);
            }
        }
    };
    
    
    // Handle USB device attached
private void handleUsbDeviceAttached(UsbDevice device) {
    if (device != null) {
        WritableMap event = Arguments.createMap();
        event.putString("eventType", "device_attached");
        event.putInt("deviceId", device.getDeviceId());
        sendEvent("usbDeviceEvent", event);
        Log.d("usbserialport", "Device attached: " + device.getDeviceId());
    } else {
        Log.w("usbserialport", "Device attached action received, but device is null.");
    }
}

// Handle USB device detached
private void handleUsbDeviceDetached(UsbDevice device) {
    if (device != null) {
        WritableMap event = Arguments.createMap();
        event.putString("eventType", "device_detached");
        event.putInt("deviceId", device.getDeviceId());
        sendEvent("usbDeviceEvent", event);
        Log.d("usbserialport", "Device detached: " + device.getDeviceId());
    } else {
        Log.w("usbserialport", "Device detached action received, but device is null.");
    }
}
    
    
}