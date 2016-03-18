package net.tplgy.closeby;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private static final Object mLock = new Object();

    private static Closeby mInstance;
    private Context mContext;
    private BluetoothManager mManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mAdvertister;
    private BluetoothLeScanner mScanner;

    private ClosebyDiscoveryListener mDiscoveryListener;
    ClosebyService mAdvertisementService;

    public static Closeby getInstance(Context context) {
        // Do we really need this synchronized?
        synchronized(mLock) {
            if (mInstance == null) {
                try {
                    mInstance = new Closeby(context);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        return mInstance;
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    private Closeby(Context context) throws Exception {
        mContext = context;
        mResults = new ArrayList<>();
        mManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mManager.getAdapter();
        if (mBluetoothAdapter == null) {
            throw new Exception("Bluetooth is not supported on this device.");
        }
    }

    public void setAdvertiseService(ClosebyService service) {
        mAdvertisementService = service;
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            log("Successfully started advertising");
            setupGattService();
        }

        public void onStartFailure(int errorCode) {
            log("Could not start advertising: " + ClosebyHelper.advertiserCode2String(errorCode));
        }
    };


    private BluetoothGattService getService() {
        BluetoothGattService service = new BluetoothGattService(mAdvertisementService.mServiceUuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        for (Map.Entry<UUID, byte[]> entry : mAdvertisementService.getProperties().entrySet()) {
            BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(entry.getKey(),
                    BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ );
            service.addCharacteristic(c);
        }

        return service;
    }

    public void connect(String deviceAddress) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress)    ;
        if (device != null) {
            log("connect to " + device.getAddress());
            device.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            log("Discovery failed: " + ClosebyHelper.scanCode2String(errorCode));
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            if (device != null) {
                if (!mResults.contains(device.getAddress())) {
                    mDiscoveryListener.onNewDevice(result.getScanRecord().getDeviceName(), device.getAddress());
                    log("Found " + result.getDevice().getAddress() + " RSSI: " + result.getRssi()
                            + ", devicename: " + result.getScanRecord().getDeviceName());
                    if (result.getScanRecord() != null && result.getScanRecord().getServiceData(ParcelUuid.fromString(mDiscoveryServices.get(0).toString())) != null) {
                        log("service data: " + byteArrayToHexString(result.getScanRecord().getServiceData(ParcelUuid.fromString(mDiscoveryServices.get(0).toString()))));
                    }
                    mResults.add(device.getAddress());
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (int i = 0; i < results.size(); i++) {
                Log.i(TAG, "Device found " + i + ": " + results.get(i).getDevice().getAddress());
            }
        }
    };

    private TextView mLogger;
    public void setLogger(TextView logger) {
        mLogger = logger;

        log("Bluetooth state:");
        log("   Address: " + mBluetoothAdapter.getAddress());
        log("   State: " + ClosebyHelper.state2String(mBluetoothAdapter.getState()));
        log("   Scan-mode: " + ClosebyHelper.scanMode2String(mBluetoothAdapter.getScanMode()));
        log("   Enabled: " + mBluetoothAdapter.isEnabled());
        log("   Name:" + mBluetoothAdapter.getName());
        log("   Discovering: " + mBluetoothAdapter.isDiscovering());
        log("   MultipleAdvertisementSupport: " + mBluetoothAdapter.isMultipleAdvertisementSupported());
        log("   OffloadedFilteringSupport: " + mBluetoothAdapter.isOffloadedFilteringSupported());
        log("   OffloadedScanBatchingSupport: " + mBluetoothAdapter.isOffloadedScanBatchingSupported());
    }

    private void log(final String msg) {
        Log.d(TAG, msg);
        if (mLogger != null) {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogger.append("\n" + DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + " " + msg);
                }
            });
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            log("GattCallback:onConnectionStateChange: [" + gatt.getDevice().getAddress() + "] status " + ClosebyHelper.status2String(status)
                    + ", state " + ClosebyHelper.connectionState2String(newState));

            super.onConnectionStateChange(gatt, status, newState);
            final int MTU = 158;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                boolean ok = gatt.requestMtu(MTU);
                log("requestMtu to " + MTU + " " + ok);
                return;
            }

            //gatt.close();
            return;
        }

        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            log("GattCallback:onMtuChanged: [" + gatt.getDevice().getAddress() + "] status " + status + ", mtu " + mtu);
            super.onMtuChanged(gatt, mtu, status);
            boolean ok = gatt.discoverServices();
            log("discoverServices " + ok);
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            log("GattCallback:onServicesDiscovered: [" + gatt.getDevice().getAddress() + "] status " + status);

            BluetoothGattService service = gatt.getService(mDiscoveryServices.get(0));
            if (service != null) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    boolean ok = gatt.readCharacteristic(characteristic);
                    log("Read " + characteristic.getUuid().toString() + ": " + Boolean.toString(ok));
                }
            } else {
                gatt.close();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            log("GattCallback:onCharacteristicRead: [" + gatt.getDevice().getAddress() + "] status: " + status + ", value: " + new String(characteristic.getValue()));

            // FIXME, should wait all characteristics done. or read characteristics one by one.
            //gatt.close();
        }
    };

    private ArrayList<String> mResults;
    private final Handler mHandler = new Handler();
    private static int SCAN_PERIOD = 10000;


    private final BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            log("BluetoothGattServerCallback:onConnectionStateChange: [" + device.getAddress() + "] status: " + status + ", state: " + ClosebyHelper.connectionState2String(newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            log("BluetoothGattServerCallback:onMtuChanged: [" + device.getAddress() + "] MTU changed to " + mtu);
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            log("BluetoothGattServerCallback:onCharacteristicReadRequest: [" + device.getAddress() + "] characteristic " + characteristic.getUuid().toString() + ", offset " + Integer.toString(offset));
            boolean done = false;
            for (Map.Entry<UUID, byte[]> entry : mAdvertisementService.getProperties().entrySet()) {
                if (characteristic.getUuid() == entry.getKey()) {
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, entry.getValue());
                    done = true;
                    break;
                }
            }

            if (!done) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            log("BluetoothGattServerCallback:onCharacteristicWriteRequest: [" + device.getAddress() + "] characteristic " + characteristic.getUuid().toString() + ", offset " + Integer.toString(offset) + ", preparedWrite: " + preparedWrite + ", responseNeeded " + responseNeeded );
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            log("BluetoothGattServerCallback:onServiceAdded: status: " + status + ", service: " + service.getUuid());
            super.onServiceAdded(status, service);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            log("BluetoothGattServerCallback:onDescriptorReadRequest: [" + device.getAddress() + "] requestId: " + requestId + ", descriptor " + descriptor.getUuid() + ", offset " + offset);
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            log("BluetoothGattServerCallback:onDescriptorWriteRequest: [" + device.getAddress() + "] requestId: " + requestId + ", descriptor " + descriptor.getUuid() + ", offset " + offset + ", preparedWrite: " + preparedWrite + ", responseNeeded " + responseNeeded );
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            log("BluetoothGattServerCallback:onExecuteWrite: [" + device.getAddress() + "] requestId: " + requestId + ", execute: " + execute);
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            log("onNotificationSent: [" + device.getAddress() + "], status: " + status);
            super.onNotificationSent(device, status);
        }
    };

    private void setupGattService()
    {
        mGattServer = mManager.openGattServer(mContext, mBluetoothGattServerCallback);

        boolean done = false;
        do {
            done = mGattServer.addService(getService());
            if (!done) {
                mGattServer.removeService(getService());
                log("add service to GattServer failed. remove it and try again.");
            }
        } while (!done);
    }

    public boolean startAdvertising() {
        if (!mBluetoothAdapter.isEnabled()) {
            log("Bluetooth is not enabled");
            return false;
        }

        mAdvertister = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mAdvertister == null) {
            log("No BLE advertiser available");
            return false;
        }

        if (mAdvertisementService == null) {
            log("No service to advertise, please setAdvertiseService.");
            return false;
        }

        log("advertise service " + mAdvertisementService);

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(ParcelUuid.fromString(mAdvertisementService.mServiceUuid.toString()));
        if (mAdvertisementService.hasData()) {
            mBluetoothAdapter.setName(new String(mAdvertisementService.mServiceData));
            dataBuilder.setIncludeDeviceName(true);
        }

        AdvertiseData advertiseData = dataBuilder.build();
        log("AdvertiseData: ");
        for (Map.Entry<ParcelUuid, byte[]> e : advertiseData.getServiceData().entrySet()) {
            log("Key: " + e.getKey().getUuid().toString() + ", value: " + byteArrayToHexString(e.getValue()));
        }

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0).build();

        mAdvertister.startAdvertising(advertiseSettings, advertiseData, mAdvertiseCallback);

        return true;
    }
    public static String byteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] hexChars = new char[bytes.length*2];
        int v;

        if (bytes == null) {
            return "null";
        }

        for(int j=0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v>>>4];
            hexChars[j*2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }
    public void stopAdvertising() {
        mAdvertisementService = null;

        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (mAdvertister != null) {
            log("Stop advertising");
            mAdvertister.stopAdvertising(mAdvertiseCallback);
            mAdvertister = null;
        }

        if (mGattServer != null) {
            mGattServer.close();
            mGattServer = null;
        }
    }

    private ArrayList<UUID> mDiscoveryServices;
    public boolean startDiscovering(ArrayList<UUID> services) {
        // cleanup previous results.
        mResults.clear();
        mDiscoveryServices = services;

        if (mDiscoveryListener != null) {
            mDiscoveryListener.onReset();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            log("Bluetooth is not enabled");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            mContext.startActivityForResult(enableBtIntent, 1);
            return false;
        }

        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();

        if (!services.isEmpty()) {
            log("Try to discover services:");
        }

        for (int i = 0; i < services.size(); i++) {
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(services.get(i))).build();
            filters.add(filter);
            log("  " + services.get(i));
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopDiscovering();
            }
        }, SCAN_PERIOD);

        mScanner.startScan(filters, settings, mScanCallback);
        log("Start discovering...");
        return true;
    }

    public void stopDiscovering() {
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (mScanner != null) {
            mScanner.stopScan(mScanCallback);
            log("Discovery stopped.");
        }
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
        mDiscoveryListener = listener;
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

