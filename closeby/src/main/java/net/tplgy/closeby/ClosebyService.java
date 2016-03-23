package net.tplgy.closeby;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by thomas on 2016-03-11.
 */
public class ClosebyService {
    private final static String SERVICE_UUID = "0000546f-0000-1000-8000-00805f9b34fb";
    private static final String TAG = "ClosebyService";
    private static final Integer BLE_ATTR_MAX_LENGTH = 512;

    private UUID mServiceUuid;
    private byte[] mServiceData;
    private Map<UUID, byte[]> mProperties = new HashMap<>();
    private boolean mReadonly = false;

    @Override
    public String toString() {
        String ret = mServiceUuid.toString();
        if (!mProperties.isEmpty()) {
            ret += "\nProperies:";
        }
        for (Map.Entry<UUID, byte[]> entry : mProperties.entrySet()) {
            ret += "\n  KEY: " + entry.getKey() + "\n  VAL: " + new String(entry.getValue());
        }
        return ret;
    }

    public ClosebyService(UUID serviceUuid) {
        mServiceUuid = serviceUuid;
    }

    public boolean setServiceData(byte[] data) {
        int MAX_SERVICE_DATA_LENGTH = 8;
        if (mServiceUuid.toString().equals(SERVICE_UUID)) {
            // we are using 16bit UUID, so we saved (128 - 16)bit = 14 bytes.
            MAX_SERVICE_DATA_LENGTH = 22;
        }

        if (data.length > MAX_SERVICE_DATA_LENGTH) {
            return false;
        }

        mServiceData = data;
        return true;
    }

    public UUID getServiceUuid() {
        return mServiceUuid;
    }

    public byte[] getServiceData() {
        return mServiceData;
    }

    public boolean addProperty(UUID key, byte[] value) {
        if (value.length > BLE_ATTR_MAX_LENGTH) {
            Log.i(TAG, "property value is too long.");
            return false;
        }

        Log.i(TAG, "add property " + key.toString() + ": " + value.toString());
        mProperties.put(key, value);
        return true;
    }

    public Map<UUID, byte[]> getProperties() {
        return mProperties;
    }

    public byte[] getValue(UUID key) {
        return mProperties.get(key);
    }

    public void setReadonly(boolean readonly) {
        mReadonly = readonly;
    }

    public boolean isReadonly() {
        return mReadonly;
    }
}
