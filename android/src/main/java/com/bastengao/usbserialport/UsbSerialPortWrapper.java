package com.bastengao.usbserialport;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;

public class UsbSerialPortWrapper implements SerialInputOutputManager.Listener {

    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 5000;

    private static final String DataReceivedEvent = "usbSerialPortDataReceived";

    private int deviceId;
    private UsbSerialPort port;
    private EventSender sender;
    private boolean closed = false;
    private SerialInputOutputManager ioManager;

    UsbSerialPortWrapper(int deviceId, UsbSerialPort port, EventSender sender) {
        this.deviceId = deviceId;
        this.port = port;
        this.sender = sender;
        this.ioManager = new SerialInputOutputManager(port, this);
        ioManager.start();
    }

    public interface EventCallback {
        void onComplete(Exception e);
    }

    public void send(byte[] data, EventCallback callback) {
        try {
            this.port.setDTR(true);
            this.port.setRTS(true);
            this.port.write(data, WRITE_WAIT_MILLIS);
            callback.onComplete(null);
        } catch (IOException e) {
            callback.onComplete(e);
        }
    }

    public void read1(int bytes, Promise promise) {
        try {
            this.port.setDTR(true);
            byte[] buffer = new byte[8192];
            int len = this.port.read(buffer, READ_WAIT_MILLIS);
            byte[] datalen = Arrays.copyOf(buffer, len);

            if (datalen.length >0){
                String hex = UsbSerialportForAndroidModule.bytesToHex(buffer);
                promise.resolve(hex);
            }
            else {
                promise.reject("read_failed", "no response from device!"+buffer.length);
            }   
        } catch (IOException e) {
            // when using read with timeout, USB bulkTransfer returns -1 on timeout _and_ errors
            // like connection loss, so there is typically no exception thrown here on error
            promise.reject("read_failed", "read failed", e);
        }
    }

    public void read(int bytes, Promise promise) {
        if (bytes <= 0) {
            promise.reject("read_failed", "expected bytes must be greater than 0");
            return;
        }
        try {
            this.port.setDTR(true);
            this.port.setRTS(true);
            int bytes1 = this.port.getReadEndpoint().getMaxPacketSize();
            byte[] buffer = new byte[bytes1];
            int read = this.port.read(buffer, 5000);
            if (read > 0) {
                byte[] data = Arrays.copyOf(buffer, read);
                String hex = UsbSerialportForAndroidModule.bytesToHex(buffer);
                promise.resolve(hex);
            } else {
                promise.reject("read_failed", "no response from device!"+buffer.length);
            }   
        } catch (IOException e) {
            promise.reject("read_failed", "read failed", e);
        }
    }

    public void onNewData(byte[] data) {
        WritableMap event = Arguments.createMap();
        String hex = UsbSerialportForAndroidModule.bytesToHex(data);
        event.putInt("deviceId", this.deviceId);
        event.putString("data", hex);
        Log.d("usbserialport", hex);
        sender.sendEvent(DataReceivedEvent, event);
    }

    public void onRunError(Exception e) {
        // TODO: implement
    }

    public void close() {
        if (closed) {
            return;
        }

        if(ioManager != null) {
            ioManager.setListener(null);
            ioManager.stop();
        }

        this.closed = true;
        try {
            port.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
