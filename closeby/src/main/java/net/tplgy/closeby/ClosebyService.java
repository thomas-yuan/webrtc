package net.tplgy.closeby;

import android.util.Log;

import java.util.Map;
import java.util.UUID;

/**
 * Created by thomas on 2016-03-11.
 */
public class ClosebyService {

    static final string TAG = "ClosebyService";
    UUID serviceUuid;
    Map<UUID, Byte[]> properties;

    ClosebyService(UUID serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public boolean addProperty(UUID key, Byte[] value) {
        if (value.length > BLE_GATTS_VAR_ATTR_LEN_MAX) {
            Log.i(TAG, "property value is too long.");
            return false;
        }

        properties.put(key, value);
        return true;
    }


    public Map<UUID, Byte[]> getProperties() {
        return properties;
    }

}
