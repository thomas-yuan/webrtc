package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;

public class ClosebyAdvertiser {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mLeAdvertister;
    private ClosebyService mAdvertisementService;
    private ClosebyLogger mLogger;

    public ClosebyAdvertiser(BluetoothAdapter adapter, ClosebyLogger logger) {
        if (adapter == null || logger == null) {
            throw new IllegalArgumentException("null parameter: " + adapter + " " + logger);
        }

        mBluetoothAdapter = adapter;
        mLogger = logger;
    }

    public boolean start(ClosebyService service) {
        mLeAdvertister = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mLeAdvertister == null) {
            mLogger.log("This device doesn't support BLE advertising.");
            return false;
        }

        mAdvertisementService = service;
        mLogger.log("Start advertising service " + mAdvertisementService);

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(new ParcelUuid(mAdvertisementService.getServiceUuid()));
        if (mAdvertisementService.getServiceData() != null) {
            // iOS doesn't support advertise service_data, so we use device_name.
            mBluetoothAdapter.setName(new String(mAdvertisementService.getServiceData()));
            dataBuilder.setIncludeDeviceName(true);
        }
        AdvertiseData advertiseData = dataBuilder.build();
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0).build();

        mLeAdvertister.startAdvertising(advertiseSettings, advertiseData, mAdvertiseCallback);
        return true;
    }

    public void stop() {
        if (mLeAdvertister != null) {
            mLogger.log("Stop advertising");
            mLeAdvertister.stopAdvertising(mAdvertiseCallback);
        }
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            mLogger.log("Advertising started.");
        }

        public void onStartFailure(int errorCode) {
            mLogger.log("Could not start advertising: " + ClosebyHelper.advertiserCode2String(errorCode));
        }
    };

    public ClosebyService getService() {
        return mAdvertisementService;
    }
}
