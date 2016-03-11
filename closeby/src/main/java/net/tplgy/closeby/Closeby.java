package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.provider.SyncStateContract;
import android.util.Log;

import java.util.ArrayList;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

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

    final static string TAG = "Closeby";
    private static Closeby mInstance = new Closeby();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mAdvertister;

    ArrayList<ClosebyService> mServices;



    public static Closeby getInstance() {

        if (mInstance.mBluetoothAdapter == null) {
            return null;
        }

        return mInstance;
    }

    private Closeby() {
        mBluetoothAdapter = ((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth is not supported on this device.");
            return;
        }

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


    }

    public boolean isAdvertisementSupported() {
        return (mAdvertister != null);
    }

    public void addAdvertiseService(ClosebyService service) {
        if (mAdvertister == null) {
            Log.d(TAG, "No BLE advertiser available, ignore advertise service");
            return;
        }

        Log.v(TAG, "Add service" + service.toString());
        mServices.add(service);
    }

    public boolean startAdvertising() {

        if (mServices.isEmpty()) {
            Log.d(TAG, "No service to advertise, please addAdvertiseService first.");
            return false;
        }

        // Is Bluetooth turned on?
        if (mBluetoothAdapter.isEnabled()) {

            if (null != mBluetoothAdapter.getBluetoothLeAdvertiser()) {
                Log.i(TAG, "");

            } else {
                // Bluetooth Advertisements are not supported.
                Log.e(TAG, R.string.bt_ads_not_supported);
            }
        } else {

            // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }



        String serviceData = "LD";
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(TPLGY_BEACON))
                .build();

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).build();

        m_advertister.startAdvertising(advertiseSettings, advertiseData, new AdvertiseCallback() {
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(LOG_TAG, "succesfully started advertising");
            }

            public void onStartFailure(int errorCode) {
                Log.e(LOG_TAG, "could not start advertising..." + Integer.toString(errorCode));
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

