package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import java.util.List;
import static java.util.Arrays.asList;

/**
 * Created by thomas on 16/03/16.
 */
public class ClosebyHelper {

    static private final String UNKNOWN = "UNKNOWN";
    static private final String SUCCESS = "SUCCESS";

    static public String scanCode2String(int errorCode) {
        final List<String> descriptions = asList("SCAN_SUCCESS",
                "SCAN_FAILED_ALREADY_STARTED",
                "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED",
                "SCAN_FAILED_INTERNAL_ERROR",
                "SCAN_FAILED_FEATURE_UNSUPPORTED",
                "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES");
        if (errorCode < descriptions.size()) {
            return descriptions.get(errorCode);
        }

        return UNKNOWN;
    }

    static public String status2String(int status) {
        if (status == 0) {
            return SUCCESS;
        }

        return UNKNOWN;
    }

    static public String advertiserCode2String(int errorCode) {
        final List<String> descriptions = asList("ADVERTISE_SUCCESS",
                "ADVERTISE_FAILED_DATA_TOO_LARGE",
                "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS",
                "ADVERTISE_FAILED_ALREADY_STARTED",
                "ADVERTISE_FAILED_INTERNAL_ERROR",
                "ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
        if (errorCode < descriptions.size()) {
            return descriptions.get(errorCode);
        }

        return UNKNOWN;
    }

    static public String connectionState2String(int state) {
        final List<String> descriptions = asList("Disconnected", "Connecting", "Connected", "Disconnecting");
        if (state < descriptions.size()) {
            return descriptions.get(state);
        }

        return Integer.toString(state);
    }

    static public String state2String(int state) {
        final String ON = "STATE_ON";
        final String OFF = "STATE_OFF";
        final String TURNING_ON = "STATE_TURNING_ON";
        final String TURNING_OFF = "STATE_TURNING_OFF";

        switch (state) {
            case BluetoothAdapter.STATE_ON:
                return ON;
            case BluetoothAdapter.STATE_OFF:
                return OFF;
            case BluetoothAdapter.STATE_TURNING_ON:
                return TURNING_ON;
            case BluetoothAdapter.STATE_TURNING_OFF:
                return TURNING_OFF;
            default:
                return UNKNOWN;
        }
    }

    static public String scanMode2String(int scanMode) {
        final String NONE = "SCAN_MODE_NONE";
        final String CONNECTABLE = "SCAN_MODE_CONNECTABLE";
        final String CONNECTABLE_DISCOVERABLE = "SCAN_MODE_CONNECTABLE_DISCOVERABLE";

        switch (scanMode) {
            case BluetoothAdapter.SCAN_MODE_NONE:
                return NONE;
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                return CONNECTABLE;
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                return CONNECTABLE_DISCOVERABLE;
            default:
                return UNKNOWN;
        }
    }
}
