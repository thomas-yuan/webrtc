package net.tplgy.closeby;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private ArrayList<BluetoothGattCharacteristic> mCharacteristics;
    private int mMTU = 20;
    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;
    private int mConnectionState = STATE_DISCONNECTED;
    private int mGattState = GATT_STATE_NEW;

    private List<byte[]> mDataQueue = new ArrayList<>();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    private static final int GATT_STATE_NEW = 0;
    private static final int GATT_STATE_SERVICE_DISCOVERED = 1;
    private static final int GATT_STATE_CHARACTERTISTIC_READ = 2;
    private static final int GATT_STATE_DONE = 3;
    private static final int GATT_STATE_INVALID = 4;

    public String toString() {
        String ret = new String("");
        if (mService.getServiceData() != null) {
            ret += new String(mService.getServiceData());
        }
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
        if (mGattState == GATT_STATE_DONE) {
            mLogger.log("already got details.");
            return;
        }

        if (mGatt == null) {
            mLogger.log("mGatt == null, connectGatt.");
            mGatt = mDevice.connectGatt(mCloseby.getContext(), false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            assert(mGatt != null);
            mConnectionState = STATE_CONNECTING;
            return;
        }

        connect();
    }

    public Map<UUID, byte[]> getProperties() {
        assert(mGattState == GATT_STATE_DONE);
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
        boolean readonly = true;
        for (BluetoothGattCharacteristic c : mCharacteristics) {
            if (c.getUuid().equals(ClosebyConstant.DATA_UUID)) {
                readonly = false;
                break;
            }
        }
        mService.setReadonly(readonly);
    }

    private void setGattState(int state) {
        mGattState = state;
        invokeNextGattAction();
    }

    private void readNextCharactertistic() {
        if (!isAllPropertyAvailable()) {
            BluetoothGattCharacteristic c = getNextCharacteristic();
            boolean ok = mGatt.readCharacteristic(c);
            mLogger.log("Read " + c.getUuid().toString() + ": " + Boolean.toString(ok));;
        } else {
            mGattState = GATT_STATE_DONE;
            mCloseby.onPeerDetailsReady(this);
        }
    }

    private boolean isAllPropertyAvailable() {
        int expect = mCharacteristics.size();
        if (!mService.isReadonly()) {
            expect -= 1;
        }

        mLogger.log("expect " + expect + " properties, we have " + mService.getPropertiesSize() + " items");
        return (mService.getPropertiesSize() == expect);
    }

    private void invokeNextGattAction() {
        switch (mGattState) {
            case GATT_STATE_NEW:
                mGatt.discoverServices();
                break;

            case GATT_STATE_SERVICE_DISCOVERED:
                BluetoothGattService s = mGatt.getService(mService.getServiceUuid());
                if (s == null) {
                    mLogger.log("Can't get GattService!!!");
                    mGattState = GATT_STATE_INVALID;
                    return;
                }
                setCharacteristics(s.getCharacteristics());
                setGattState(GATT_STATE_CHARACTERTISTIC_READ);
                break;

            case GATT_STATE_CHARACTERTISTIC_READ:
                readNextCharactertistic();
                break;

            case GATT_STATE_DONE:
                mLogger.log("All done.");
                if (!mDataQueue.isEmpty()) {
                    //FIXME Send queue data.
                    byte[] data = mDataQueue.remove(0);
                    send(data);
                }
                break;

            default:
                mLogger.log("Invalid status.");
        }
    }

    public void onConnected() {
        mConnectionState = STATE_CONNECTED;
        invokeNextGattAction();
    }

    public void onDisconnected() {
        mConnectionState = STATE_DISCONNECTED;
    }


    public byte[] getProperty(UUID uuid) {
        return mService.getValue(uuid);
    }


    public byte[] getServiceData() {
        return mService.getServiceData();
    }

    public boolean isReadonly() {
        return false;
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

    public void setProperty(UUID uuid, byte[] value) {
        mService.addProperty(uuid, value);
    }

    private void connect() {
        if (!mGatt.connect()) {
            mLogger.log("Gatt.connect() failed.");
            return;
        }

        mConnectionState = STATE_CONNECTING;
    }

    private boolean send(byte[] data) {
        BluetoothGattCharacteristic c = mGatt.getService(mService.getServiceUuid()).getCharacteristic(ClosebyConstant.DATA_UUID);
        if (c == null) {
            mLogger.log("service doesn't support receiving data.");
            return false;
        }

        c.setValue(data);
        boolean r = mGatt.writeCharacteristic(c);
        mLogger.log("WriteCharacteristic " + r);
        return true;
    }

    public boolean sendData(byte[] data) {
        if (mConnectionState == STATE_CONNECTED) {
            if (mDataQueue.size() == 0) {
                return send(data);
            }

            mDataQueue.add(data);
            return true;
        }

        mDataQueue.add(data);
        connect();

        return true;

//        if (mConnectionState == STATE_CONNECTED) {
//            if (mGatt != null) {
//                BluetoothGattCharacteristic c = .getCharacteristic(ClosebyConstant.DATA_UUID);
//                if (c == null) {
//                    mLogger.log("service doesn't support receiving data.");
//                    gatt.close();
//                    return;
//                }
//
//                c.setValue(peer.getData());
//                boolean r = gatt.writeCharacteristic(c);
//                mLogger.log("WriteCharacteristic " + r);
//            } else {
//            }
//
//        }
//
//            peer.getGatt().writeCharacteristic()
//            if (null == ) {
//                return false;
//            }
//
//            mLogger.log("This connection is for sending data");
//            peer.setData(null);
//            return;
//            return true;
//        }
    }




private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
        mLogger.log("GattCallback:onConnectionStateChange: [" + gatt.getDevice().getAddress() + "] status " + ClosebyHelper.status2String(status)
                + ", state " + ClosebyHelper.connectionState2String(newState));

        super.onConnectionStateChange(gatt, status, newState);
        ClosebyPeer peer = mCloseby.getPeerByAddress(gatt.getDevice().getAddress());
        if (peer == null) {
            mLogger.log("Can't find peer!!!");
            return;
        }

        peer.setGatt(gatt);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                peer.onConnected();
                break;

            case BluetoothProfile.STATE_DISCONNECTED:
                peer.onDisconnected();
                break;
            default:
        }
    }

    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        mLogger.log("GattCallback:onMtuChanged: [" + gatt.getDevice().getAddress() + "] status " + status + ", mtu " + mtu);
        super.onMtuChanged(gatt, mtu, status);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        mLogger.log("GattCallback:onServicesDiscovered: [" + gatt.getDevice().getAddress() + "] status " + status);

        ClosebyPeer peer = mCloseby.getPeerByAddress(gatt.getDevice().getAddress());
        gatt.getService(peer.getService().getServiceUuid());
        assert(peer != null);
        peer.setGattState(GATT_STATE_SERVICE_DISCOVERED);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status != 0) {
            mLogger.log("onCharacteristicRead failed");
            return;
        }

        mLogger.log("GattCallback:onCharacteristicRead: [" + gatt.getDevice().getAddress() + "] status: " + status + ", value: " + new String(characteristic.getValue()));
        ClosebyPeer peer = mCloseby.getPeerByAddress(gatt.getDevice().getAddress());
        assert(peer != null);

        peer.setProperty(characteristic.getUuid(), characteristic.getValue());
        peer.invokeNextGattAction();
    }


    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status != 0) {
            mLogger.log("onCharacteristicWrite failed");
            return;
        }

        mLogger.log("GattCallback:onCharacteristicWrite: [" + gatt.getDevice().getAddress() + "] status: " + status + ", value: " + new String(characteristic.getValue()));
        ClosebyPeer peer = mCloseby.getPeerByAddress(gatt.getDevice().getAddress());
        assert(peer != null);
        peer.invokeNextGattAction();
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
