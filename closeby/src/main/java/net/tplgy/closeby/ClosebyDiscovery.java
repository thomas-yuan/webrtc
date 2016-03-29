package net.tplgy.closeby;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ClosebyDiscovery {
    private Closeby mCloseby;
    private ClosebyLogger mLogger;
    private BluetoothLeScanner mScanner;
    private UUID mService;
    private Set<String> mOldDiscoveredDeviceAddresses = new HashSet<>();
    private Set<String> mNewDiscoveredDeviceAddresses = new HashSet<>();
    private final Handler mHandler = new Handler();
    private final Runnable mStopScanning = new Runnable() {
        @Override
        public void run() {
            stop();
            updateDispearedPeers();
        }
    };

    private boolean mScanning;

    public ClosebyDiscovery(Closeby closeby, ClosebyLogger logger) {
        if (closeby == null || logger == null) {
            throw new IllegalArgumentException("null parameter: " + closeby + ", " + logger);
        }

        mCloseby = closeby;
        mLogger = logger;
    }

    private void updateDispearedPeers() {
        for (String p : mOldDiscoveredDeviceAddresses) {
            if (!mNewDiscoveredDeviceAddresses.contains(p)) {
                mLogger.log(p + " dispeared");
                mCloseby.onPeerDisappeared(p);
            }
        }
    }

    public void stop() {
        mScanning = false;
        if (mScanner != null) {
            mScanner.stopScan(mScanCallback);
            mLogger.log("Discovering stopped.");
        }
    }

    public boolean start(UUID service) {
        int SCAN_PERIOD = 10000;

        if (service == null) {
            throw new InvalidParameterException("null parameter");
        }

        if (mScanning) {
            mHandler.removeCallbacks(mStopScanning);
            stop();
        }

        mService = service;
        mOldDiscoveredDeviceAddresses = new HashSet<>(mNewDiscoveredDeviceAddresses);
        mNewDiscoveredDeviceAddresses = new HashSet<>();

        if (mScanner == null) {
            mScanner = mCloseby.getAdapter().getBluetoothLeScanner();
            if (mScanner == null) {
                mLogger.log("Can't get LeScanner.");
                return false;
            }
        }

        mScanning = true;
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(service)).build());

        mHandler.postDelayed(mStopScanning, SCAN_PERIOD);
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

            if (!mNewDiscoveredDeviceAddresses.contains(device.getAddress())) {
                mNewDiscoveredDeviceAddresses.add(device.getAddress());
                if (!mOldDiscoveredDeviceAddresses.contains(device.getAddress())) {
                    ClosebyPeer peer = new ClosebyPeer(mCloseby, device, mLogger);
                    ClosebyService service = new ClosebyService(mService);
                    service.setServiceData(device.getName().getBytes());
                    peer.setService(service);
                    peer.setRssi(result.getRssi());
                    mCloseby.onPeerDiscovered(peer);
                } else {
                    ClosebyPeer peer = mCloseby.getPeerByAddress(device.getAddress());
                    peer.setRssi(result.getRssi());
                    mCloseby.onPeerDiscovered(peer);
                    mLogger.log("Already found it before.");
                }

                mLogger.log("Found [" + device.getAddress() + "] " + device.getName() + " RSSI: " + result.getRssi());
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
