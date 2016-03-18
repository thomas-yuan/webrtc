package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by thomas on 18/03/16.
 */
public class ClosebyDiscovery {
    private BluetoothAdapter mBluetoothAdapter;
    private ClosebyDiscoveryListener mListener;
    private ClosebyLogger mLogger;
    private BluetoothLeScanner mScanner;
    private UUID mService;
    private ArrayList<ClosebyService> mDiscoveredServices = new ArrayList<ClosebyService>();
    private ArrayList<String> mResults = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private static int SCAN_PERIOD = 10000;

    public ClosebyDiscovery(BluetoothAdapter adapter, ClosebyDiscoveryListener listener, ClosebyLogger logger) {
        if (adapter == null || listener == null || logger == null) {
            throw new IllegalArgumentException("null parameter: " + adapter + ", " + listener + ", " + logger);
        }

        mBluetoothAdapter = adapter;
        mListener = listener;
        mLogger = logger;
    }

    public void stop() {
        if (mScanner != null) {
            mScanner.stopScan(mScanCallback);
            mLogger.log("Discovering stopped.");
        }
    }

    public boolean start(UUID service) {
        mService = service;
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(service)).build());

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, SCAN_PERIOD);

        mScanner.startScan(filters, settings, mScanCallback);
        mLogger.log("Discovering started");
        return true;
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            mLogger.log("Discovery failed: " + ClosebyHelper.scanCode2String(errorCode));
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null) {
                mLogger.log("Device is null!!!!");
                return;
            }

            if (!mResults.contains(device.getAddress())) {
                mListener.onNewDevice(device.getName(), device.getAddress());
                mLogger.log("Found [" + device.getAddress() + "] " + device.getName() + " RSSI: " + result.getRssi());
                mResults.add(device.getAddress());
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            BluetoothDevice device;
            for (int i = 0; i < results.size(); i++) {
                device = results.get(i).getDevice();
                mLogger.log("BatchScanResults " + i + ": [" + device.getAddress() + "] " + device.getName());
            }
        }
    };
}
