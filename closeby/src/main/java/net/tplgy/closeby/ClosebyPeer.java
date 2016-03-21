package net.tplgy.closeby;

import android.bluetooth.BluetoothGattCharacteristic;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by thomas on 2016-03-11.
 */
public class ClosebyPeer {
    private UUID mService;
    private ClosebyLogger mLogger;
    private String mAddress;
    private byte[] mServiceData;
    private int mRSSI;
    private boolean mInfoReady;
    private int mCurrentPropertyIndex = 0;
    private ClosebyPeerListener mListener;
    private ArrayList<BluetoothGattCharacteristic> mCharacteristics;
    private Map<UUID, byte[]> mAvalibleProperties = new HashMap<>();
    private boolean mReadonly;

    public String toString() {
        String ret = new String(mServiceData);
        ret += " [" + mAddress + "]" + " RSSI " + mRSSI;
        return ret;
    }

    ClosebyPeer(UUID service, String address, byte[] serviceData, int RSSI, ClosebyLogger logger) {
        if (address == null || logger == null) {
            throw new InvalidParameterException("null parameter " + address + " " + logger);
        }

        mService = service;
        mAddress = address;
        mServiceData = serviceData;
        mRSSI = RSSI;
        mLogger = logger;
        mReadonly = true;
        logger.log("New peer created: [" + mAddress + "] " + " RSSI " + mRSSI);
    }

    public String getAddress() {
        return mAddress;
    }

    public byte[] getServiceData() {
        return mServiceData;
    }

    public UUID getService() {
        return mService;
    }

    public boolean isAllPropertyAvalible() {
        if (mCharacteristics == null) {
            mLogger.log("mCharacteristics is null");
            return false;
        }
        assert (mAvalibleProperties != null);
        mLogger.log("Characteristics: " + mCharacteristics.size() + ", Properties: " + mAvalibleProperties.size());
        return (mCharacteristics.size() == mAvalibleProperties.size());
    }

    public BluetoothGattCharacteristic getNextCharacteristic() {
        return mCharacteristics.get(mCurrentPropertyIndex++);
    }

    public void setProperty(UUID uuid, byte[] value) {
        mAvalibleProperties.put(uuid, value);
        if (isAllPropertyAvalible()) {
            mListener.onReady(this);
        }
    }

    public byte[] getProperty(UUID uuid) {
        return mAvalibleProperties.get(uuid);
    }

    public Map<UUID, byte[]> getProperties() {
        return mAvalibleProperties;
    }

    public void setCharacteristics(List<BluetoothGattCharacteristic> characteristics) {
        mCharacteristics = (ArrayList)characteristics;
        mLogger.log("setCharacteristics " + characteristics.size());
        for (BluetoothGattCharacteristic c : mCharacteristics) {
            if (c.getUuid().equals(ClosebyConstant.DATA_UUID)) {
                mCharacteristics.remove(c);
                mLogger.log("Remove DATA_IN_UUID from characteristics");
                mReadonly = false;
                break;
            }
        }
    }

    public void onConnectionFailed() {
        mListener.onFailed(this);
    }

    public void setListener(ClosebyPeerListener listener) {
        mListener = listener;
    }

    public boolean isReadonly() {
        return mReadonly;
    }

    private byte[] mData;
    public void setData(byte[] data) {
        mData = data;
    }

    public byte[] getData() {
        return mData;
    }
}
