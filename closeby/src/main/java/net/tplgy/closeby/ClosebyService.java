package net.tplgy.closeby;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by thomas on 2016-03-11.
 */
public class ClosebyService {

    private static final String TAG = "ClosebyService";
    private static final Integer BLE_ATTR_MAX_LENGTH = 512;

    UUID mServiceUuid;
    Map<UUID, byte[]> mProperties;

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
        this.mServiceUuid = serviceUuid;
        mProperties = new HashMap<>();
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
}
