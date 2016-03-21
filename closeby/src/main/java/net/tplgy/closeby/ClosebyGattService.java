package net.tplgy.closeby;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.util.Map;
import java.util.UUID;

/**
 * Created by thomas on 18/03/16.
 */
public class ClosebyGattService {
    private Context mContext;
    private BluetoothManager mManager;
    private BluetoothGattServer mGattServer;
    private ClosebyLogger mLogger;
    private ClosebyService mService;

    public ClosebyGattService(Context context, ClosebyLogger logger) {
        if (context == null || logger == null) {
            throw new IllegalArgumentException("null parameter: " + context + " " + logger);
        }

        mContext = context;
        mLogger = logger;
        mManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public boolean serve(ClosebyService service) {
        mService = service;
        mGattServer = mManager.openGattServer(mContext, mBluetoothGattServerCallback);

        BluetoothGattService s = new BluetoothGattService(service.mServiceUuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        for (Map.Entry<UUID, byte[]> entry : service.getProperties().entrySet()) {
            BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(entry.getKey(),
                    BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            s.addCharacteristic(c);
        }

        // service has a listener, accept write
        if (service.mListener != null) {
            BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(ClosebyConstant.DATA_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            s.addCharacteristic(c);
        }

        if (!mGattServer.addService(s)) {
            mLogger.log("Add service to GattServer failed.");
            return false;
        }

        return true;
    }

    public void stop() {
        mGattServer.close();
    }


    private final BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            mLogger.log("BluetoothGattServerCallback:onConnectionStateChange: [" + device.getAddress() + "] status: " + status + ", state: " + ClosebyHelper.connectionState2String(newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            mLogger.log("BluetoothGattServerCallback:onMtuChanged: [" + device.getAddress() + "] MTU changed to " + mtu);
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            mLogger.log("BluetoothGattServerCallback:onCharacteristicReadRequest: [" + device.getAddress() + "] characteristic " + characteristic.getUuid().toString() + ", offset " + Integer.toString(offset));
            byte[] val = mService.getValue(characteristic.getUuid());
            if (val != null) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, val);
            } else {
                // TODO, when we should call super? when we can't handle it?
                mLogger.log("No value for characteristic " + characteristic.getUuid());
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            mLogger.log("BluetoothGattServerCallback:onCharacteristicWriteRequest: [" + device.getAddress() + "] characteristic " + characteristic.getUuid().toString() + ", offset " + Integer.toString(offset) + ", preparedWrite: " + preparedWrite + ", responseNeeded " + responseNeeded);
            // TODO.
            //super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            assert (characteristic.getUuid() == ClosebyConstant.DATA_UUID);
            ClosebyPeer peer = new ClosebyPeer(mService.mServiceUuid, device.getAddress(), null, 0, mLogger);
            mLogger.log("DATA received from " + device.getAddress() + ": " + new String(value));
            mService.mListener.dataReceived(peer, value);
            if (responseNeeded) {
                mLogger.log("send write response");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            mLogger.log("BluetoothGattServerCallback:onServiceAdded: service: " + service.getUuid() + ", status: " + status);
            super.onServiceAdded(status, service);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            mLogger.log("BluetoothGattServerCallback:onDescriptorReadRequest: [" + device.getAddress() + "] requestId: " + requestId + ", descriptor " + descriptor.getUuid() + ", offset " + offset);
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            mLogger.log("BluetoothGattServerCallback:onDescriptorWriteRequest: [" + device.getAddress() + "] requestId: " + requestId + ", descriptor " + descriptor.getUuid() + ", offset " + offset + ", preparedWrite: " + preparedWrite + ", responseNeeded " + responseNeeded);
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            mLogger.log("BluetoothGattServerCallback:onExecuteWrite: [" + device.getAddress() + "] requestId: " + requestId + ", execute: " + execute);
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            mLogger.log("onNotificationSent: [" + device.getAddress() + "], status: " + status);
            super.onNotificationSent(device, status);
        }
    };
}
