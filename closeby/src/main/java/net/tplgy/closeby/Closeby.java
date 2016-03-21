package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.UUID;

public class Closeby {

    final static String TAG = "Closeby";
    private static final Object mLock = new Object();
    private static Closeby mInstance;

    private Context mContext;
    private BluetoothManager mManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ClosebyAdvertiser mAdvertisor;
    private ClosebyDiscovery mDiscovery;
    private ClosebyGattService mGattService;
    private ClosebyGattClient mGattClient;
    private ClosebyDiscoveryListener mDiscoveryListener;
    private ClosebyLogger mLogger;

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

    private Closeby(Context context) throws Exception {
        mContext = context;
        mManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mManager.getAdapter();
        if (mBluetoothAdapter == null) {
            throw new Exception("Bluetooth is not supported on this device.");
        }
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public ClosebyPeer getPeerbyAddress(String address) {
        if (mDiscovery != null) {
            return mDiscovery.getPeerbyAddress(address);
        }

        assert (mAdvertisor != null);
        return new ClosebyPeer(mAdvertisor.getService().mServiceUuid, address, null, 0, mInternalLogger);
    }

    public boolean getProperties(ClosebyPeer peer, ClosebyPeerListener listener) {
        if (mGattClient == null) {
            mGattClient = new ClosebyGattClient(mContext, mBluetoothAdapter, mInternalLogger);
        }

        mGattClient.getProperties(peer, listener);
        return true;
    }

    private void generateStateLog() {
        mInternalLogger.log("Bluetooth state:");
        mInternalLogger.log("   Address: " + mBluetoothAdapter.getAddress());
        mInternalLogger.log("   State: " + ClosebyHelper.state2String(mBluetoothAdapter.getState()));
        mInternalLogger.log("   Scan-mode: " + ClosebyHelper.scanMode2String(mBluetoothAdapter.getScanMode()));
        mInternalLogger.log("   Enabled: " + mBluetoothAdapter.isEnabled());
        mInternalLogger.log("   Name:" + mBluetoothAdapter.getName());
        mInternalLogger.log("   Discovering: " + mBluetoothAdapter.isDiscovering());
        mInternalLogger.log("   MultipleAdvertisementSupport: " + mBluetoothAdapter.isMultipleAdvertisementSupported());
        mInternalLogger.log("   OffloadedFilteringSupport: " + mBluetoothAdapter.isOffloadedFilteringSupported());
        mInternalLogger.log("   OffloadedScanBatchingSupport: " + mBluetoothAdapter.isOffloadedScanBatchingSupported());
    }

    public void setLogger(ClosebyLogger logger) {
        mLogger = logger;
        generateStateLog();
    }

    final private ClosebyLogger mInternalLogger = new ClosebyLogger() {
        @Override
        public void log(String logs) {
            Log.d(TAG, logs);
            if (mLogger != null) {
                mLogger.log(DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + " " + logs);
            }
        }
    };

    public boolean startAdvertising(ClosebyService service) {
        if (!mBluetoothAdapter.isEnabled()) {
            mInternalLogger.log("Bluetooth is disabled");
            return false;
        }

        if (mGattService == null) {
            mGattService = new ClosebyGattService(mContext, mInternalLogger);
        }

        if (!mGattService.serve(service)) {
            return false;
        }

        if (mAdvertisor == null) {
            mAdvertisor = new ClosebyAdvertiser(mBluetoothAdapter, mInternalLogger);
        }

        if (!mAdvertisor.start(service)) {
            mGattService.stop();
            return false;
        }

        return true;
    }

    public void stopAdvertising() {
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (mGattService != null) {
            mGattService.stop();
        }

        if (mAdvertisor != null) {
            mAdvertisor.stop();
        }
    }

    public boolean startDiscovering(UUID service, ClosebyDiscoveryListener listener) {
        if (service == null || listener == null) {
            mInternalLogger.log("null parameter: " + service + " " + listener);
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mInternalLogger.log("Bluetooth is disabled.");
            return false;
        }

        if (mDiscovery == null) {
            mDiscovery = new ClosebyDiscovery(mBluetoothAdapter, listener, mInternalLogger);
        }

        return mDiscovery.start(service);
    }

    public void stopDiscovering() {
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (mDiscovery != null) {
            mDiscovery.stop();
        }
    }

    public boolean sendDataToPeer(ClosebyPeer peer, byte[] bytes) {

        assert (mGattClient != null);

        if (peer.isReadonly()) {
            mInternalLogger.log("peer " + peer.getAddress() + " is readonly, can't send data to it");
            return false;
        }

        peer.setData(bytes);
        return mGattClient.sendData(peer, bytes);
        }

    public boolean sendFileToPeer(ClosebyPeer peer, String filePath, String id) {

        return true;
    }

    public boolean cancelSendingFile(String id) {

        return true;
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

