package net.tplgy.closeby;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by thomas on 2016-03-11.
 */
public class ClosebyPeer {
    private Closeby mCloseby;
    private ClosebyService mService;
    private ClosebyLogger mLogger;
    private int mRSSI;
    private int mCurrentPropertyIndex = 0;
    private ClosebyPeerListener mListener;
    private ArrayList<BluetoothGattCharacteristic> mCharacteristics;
    private int mMTU = 20;
    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;
    private int mConnectionState = STATE_DISCONNECTED;
    private boolean mQueryDone = false;

    private List<byte[]> mDataQueue = new ArrayList<>();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private void details;


    public String toString() {
        String ret = new String(mService.getServiceData());
        ret += " [" + mDevice.getAddress() + "]" + " RSSI " + mRSSI;
        return ret;
    }

    ClosebyPeer(Closeby closeby, BluetoothDevice device, ClosebyLogger logger) {
        if (closeby == null || device == null || logger == null) {
            throw new InvalidParameterException("null parameter " + closeby + ", " + device + " " + logger);
        }

        mCloseby = closeby;
        mDevice = device;
        mLogger = logger;
        logger.log("New peer created: [" + mDevice.getAddress() + "]");
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public void getDetails() {
        if (mQueryDone) {
            mLogger.log("already got details.");
            return;
        }

        if (mGatt == null) {
            mLogger.log("mGatt == null, connectGatt.");
            mGatt = mDevice.connectGatt(mCloseby.getContext(), false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            mConnectionState = STATE_CONNECTING;
            return;
        }

        if (!mGatt.connect()) {
            mLogger.log("Gatt.connect() failed.");
            return;
        }

        mConnectionState = STATE_CONNECTING;
        return;
    }

    public Map<UUID, byte[]> getProperties() {
        assert(mQueryDone);
        return mService.getProperties();
    }

    public void setService(ClosebyService service) {
        mService = service;
    }

    public ClosebyService getService() {
        return mService;
    }

    public BluetoothGattCharacteristic getNextCharacteristic() {
        BluetoothGattCharacteristic c = mCharacteristics.get(mCurrentPropertyIndex++);
        if (c != null && c.getUuid().equals(ClosebyConstant.DATA_UUID)) {
            c = mCharacteristics.get(mCurrentPropertyIndex++);
        }

        return c;
    }

    public void setCharacteristics(List<BluetoothGattCharacteristic> characteristics) {
        mCharacteristics = (ArrayList)characteristics;
        mLogger.log("setCharacteristics " + characteristics.size());
        for (BluetoothGattCharacteristic c : mCharacteristics) {
            if (c.getUuid().equals(ClosebyConstant.DATA_UUID)) {
                mReadonly = false;
                break;
            }
        }
    }

    public void onConnectionFailed() {
        mConnected = false;
        mListener.onFailed(this);
    }

    public void onReady() {
        mListener.onReady(this);
    }

    public void setListener(ClosebyPeerListener listener) {
        mListener = listener;
    }

    public boolean isReadonly() {
        return mReadonly;
    }

    public void setMTU(int mtu) {
        mMTU = mtu;
    }

    public int getMTU() {
        return mMTU;
    }

    public void setGatt(BluetoothGatt gatt) {
        mGatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public void setRssi(int rssi) {
        mRSSI = rssi;
    }
    public int getRssi() {
        return mRSSI;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public boolean sendData(byte[] data) {

        if (mConnected) {
            if (mGatt != null) {
                BluetoothGattCharacteristic c = .getCharacteristic(ClosebyConstant.DATA_UUID);
                if (c == null) {
                    mLogger.log("service doesn't support receiving data.");
                    gatt.close();
                    return;
                }

                c.setValue(peer.getData());
                boolean r = gatt.writeCharacteristic(c);
                mLogger.log("WriteCharacteristic " + r);
            } else {
            }

        }

            peer.getGatt().writeCharacteristic()
            if (null == ) {
                return false;
            }

            mLogger.log("This connection is for sending data");
            peer.setData(null);
            return;
            return true;
        }
    }


private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
        mLogger.log("GattCallback:onConnectionStateChange: [" + gatt.getDevice().getAddress() + "] status " + ClosebyHelper.status2String(status)
                + ", state " + ClosebyHelper.connectionState2String(newState));

        super.onConnectionStateChange(gatt, status, newState);
        ClosebyPeer peer = mCloseby.getPeerByAddress(gatt.getDevice().getAddress());

        final int MTU = 256;
        if (newState == BluetoothProfile.STATE_CONNECTED) {

            boolean ok = gatt.requestMtu(MTU);
            mLogger.log("requestMtu to " + MTU + " " + ok);
            return;
        }

        ClosebyPeer peer = mPeers.get(gatt.getDevice().getAddress());
        if (peer == null) {
            mLogger.log("never request this peer?!");
            return;
        }

        peer.onConnectionFailed();
        return;
    }

    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        mLogger.log("GattCallback:onMtuChanged: [" + gatt.getDevice().getAddress() + "] status " + status + ", mtu " + mtu);
        super.onMtuChanged(gatt, mtu, status);
        boolean ok = gatt.discoverServices();
        mLogger.log("discoverServices " + ok);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        mLogger.log("GattCallback:onServicesDiscovered: [" + gatt.getDevice().getAddress() + "] status " + status);

        ClosebyPeer peer = mPeers.get(gatt.getDevice().getAddress());
        if (peer == null) {
            mLogger.log("never request this peer?!");
            gatt.close();
            return;
        }

        BluetoothGattService service = gatt.getService(peer.getService());
        if (service == null) {
            mLogger.log("peer doesn't have service " + peer.getService() + "?!");
            gatt.close();
            return;
        }

        byte[] data = peer.getData();
        if (data != null) {
            mLogger.log("This connection is for sending data");
            BluetoothGattCharacteristic c = service.getCharacteristic(ClosebyConstant.DATA_UUID);
            if (c == null) {
                mLogger.log("service doesn't support receiving data.");
                gatt.close();
                return;
            }

            c.setValue(peer.getData());
            boolean r = gatt.writeCharacteristic(c);
            mLogger.log("WriteCharacteristic " + r);
            peer.setData(null);
            return;
        }

        peer.setCharacteristics(service.getCharacteristics());
        if (peer.isAllPropertyAvailable()) {
            gatt.close();
        } else {
            BluetoothGattCharacteristic c = peer.getNextCharacteristic();
            boolean ok = gatt.readCharacteristic(c);
            mLogger.log("Read " + c.getUuid().toString() + ": " + Boolean.toString(ok));;
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        mLogger.log("GattCallback:onCharacteristicRead: [" + gatt.getDevice().getAddress() + "] status: " + status + ", value: " + new String(characteristic.getValue()));

        ClosebyPeer peer = mPeers.get(gatt.getDevice().getAddress());
        if (peer == null) {
            mLogger.log("never request this peer?!");
            gatt.close();
            return;
        }

        peer.setProperty(characteristic.getUuid(), characteristic.getValue());
        if (peer.isAllPropertyAvailable()) {
            mLogger.log("We get all characteristics.");
            peer.onReady();
            gatt.close();
        } else {
            BluetoothGattCharacteristic c = peer.getNextCharacteristic();
            boolean ok = gatt.readCharacteristic(c);
            mLogger.log("Read " + c.getUuid().toString() + ": " + Boolean.toString(ok));;
        }
    }


    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        mLogger.log("GattCallback:onCharacteristicWrite: [" + gatt.getDevice().getAddress() + "] status: " + status + ", value: " + new String(characteristic.getValue()));
    }

    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
    }

    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                 int status) {
    }

    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                  int status) {
    }

    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        mLogger.log("Write done! " + status);

    }


    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    }

};

}
