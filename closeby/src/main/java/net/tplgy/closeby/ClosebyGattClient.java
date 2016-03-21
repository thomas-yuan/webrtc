package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.HashMap;

/**
 * Created by thomas on 21/03/16.
 */
public class ClosebyGattClient {

    private Context mContext;
    private BluetoothAdapter mAdapter;
    private ClosebyLogger mLogger;

    private HashMap<String, ClosebyPeer> mPeers = new HashMap<>();

    public ClosebyGattClient(Context context, BluetoothAdapter adapter, ClosebyLogger logger) {
        mContext = context;
        mAdapter = adapter;
        mLogger = logger;
    }

    public void getProperties(ClosebyPeer peer, ClosebyPeerListener listener) {
        BluetoothDevice device = mAdapter.getRemoteDevice(peer.getAddress());
        if (device == null) {
            mLogger.log("peer " + peer.getAddress() + " is out of range");
            listener.onFailed(peer);
            return;
        }

        mLogger.log("connect to " + peer.getAddress());
        peer.setListener(listener);
        mPeers.put(peer.getAddress(), peer);
        device.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            mLogger.log("GattCallback:onConnectionStateChange: [" + gatt.getDevice().getAddress() + "] status " + ClosebyHelper.status2String(status)
                    + ", state " + ClosebyHelper.connectionState2String(newState));

            super.onConnectionStateChange(gatt, status, newState);
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
            if (peer.isAllPropertyAvalible()) {
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
            if (peer.isAllPropertyAvalible()) {
                mLogger.log("We get all characteristics.");
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

    public boolean sendData(ClosebyPeer peer, byte[] bytes) {

        BluetoothDevice device = mAdapter.getRemoteDevice(peer.getAddress())    ;
        if (device == null) {
            mLogger.log("peer " + peer.getAddress() + " is out of range");
            return false;
        }

        if (null == device.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)) {
            return false;
        }

        return true;
    }
}
