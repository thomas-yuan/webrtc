package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by thomas on 2016-03-11.
 */
public class Closeby {

    public enum ConnectionState {
        DISCOVERED,
        NEARBY,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    };

    final static String TAG = "Closeby";

    private static Closeby mInstance;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mAdvertister;

    ArrayList<ClosebyService> mServices;

    public static Closeby getInstance(Context context) {

        if (mInstance == null) {
            mInstance = new Closeby(context);
        }

        if (mInstance.mBluetoothAdapter == null) {
            return null;
        }

        return mInstance;
    }

    private Closeby(Context context) {
        mContext = context;
        mBluetoothAdapter = ((BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth is not supported on this device.");
            return;
        }

        Log.i(TAG, "Bluetooth state:\n   Address: " + mBluetoothAdapter.getAddress()
                + "\n   State: " + mBluetoothAdapter.getState()
                + "\n   Scan-mode: " + mBluetoothAdapter.getScanMode()
                + "\n   Enabled: " + mBluetoothAdapter.isEnabled()
                + "\n   Name:" + mBluetoothAdapter.getName()
                + "\n   Discovering: " + mBluetoothAdapter.isDiscovering()
                + "\n   MultipleAdvertisementSupport: " + mBluetoothAdapter.isMultipleAdvertisementSupported()
                + "\n   OffloadedFilteringSupport: " + mBluetoothAdapter.isOffloadedFilteringSupported()
                + "\n   OffloadedScanBatchingSupport: " + mBluetoothAdapter.isOffloadedScanBatchingSupported());

        if (mBluetoothAdapter.isEnabled()) {

            mAdvertister = mBluetoothAdapter.getBluetoothLeAdvertiser();
            if (mAdvertister == null) {
                Log.i(TAG, "No BLE advertiser available");
            }

        } else {

            // TODO: Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, SyncStateContract.Constants.REQUEST_ENABLE_BT);
        }

        mServices = new ArrayList<>();

    }

    public boolean isAdvertisementSupported() {
        return (mAdvertister != null);
    }

    public void addAdvertiseService(ClosebyService service) {
        if (mAdvertister == null) {
            Log.d(TAG, "No BLE advertiser available, ignore advertise service");
            return;
        }

        Log.v(TAG, "Add service: " + service.toString());
        mServices.add(service);
    }

    public boolean startAdvertising() {
        if (mAdvertister == null) {
            Log.d(TAG, "No BLE advertiser available, ignore advertise service");
            return false;
        }

        if (mServices.isEmpty()) {
            Log.d(TAG, "No service to advertise, please addAdvertiseService first.");
            return false;
        }

        Log.d(TAG, "advertise service: " + mServices.get(0).mServiceUuid.toString());
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        dataBuilder.addServiceUuid(ParcelUuid.fromString(mServices.get(0).mServiceUuid.toString()));
        dataBuilder.setIncludeDeviceName(true);
        AdvertiseData advertiseData = dataBuilder.build();

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH).build();

        mAdvertister.startAdvertising(advertiseSettings, advertiseData, new AdvertiseCallback() {
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "succesfully started advertising");
            }

            public void onStartFailure(int errorCode) {
                Log.e(TAG, "could not start advertising..." + Integer.toString(errorCode));
            }
        });

        return true;
    }

    public void stopAdvertising() {

    }

    public void startDiscovering() {

    }

    public void stopDiscovering() {

    }

    public boolean sendDataToPeer(ClosebyPeer peer, Byte[] bytes) {

        return true;
    }

    public boolean sendFileToPeer(ClosebyPeer peer, String filePath, String id) {

        return true;
    }

    public boolean cancelSendingFile(String id) {

        return true;
    }

    public void addDiscoveryListener(ClosebyDiscoveryListener listener) {

    }

    public void removeDiscoveryListener(ClosebyDiscoveryListener listener) {

    }

    public void addDataTransferListener(ClosebyFileTransferListener listener) {

    }

    public void removeDataTransferListener(ClosebyFileTransferListener listener) {

    }

    public void addFileTransferListener(ClosebyFileTransferListener listener) {

    }

    public void removeFileTransferListener(ClosebyFileTransferListener listener) {

    }
}

