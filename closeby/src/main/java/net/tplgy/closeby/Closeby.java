package net.tplgy.closeby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Closeby {

    final static String TAG = "Closeby";
    private static Closeby mInstance;

    private Context mContext;
    private BluetoothManager mManager;
    private BluetoothAdapter mBluetoothAdapter;

    private ClosebyAdvertiser mAdvertiser;
    private ClosebyDiscovery mDiscovery;

    private ClosebyGattService mGattService;

    // If someone connect to us and we didn't find it before, we will treat it as "discovered".
    private ClosebyDiscoveryListener mDiscoveryListener;
    ClosebyDataTransferListener mDataTransferListener;
    private Map<String, ClosebyPeer> mPeers = new HashMap<>();
    private ClosebyLogger mLogger;

    public static synchronized Closeby getInstance(Context context) {
        if (mInstance == null) {
            try {
                mInstance = new Closeby(context);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
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

    public void setDiscoveryListener(ClosebyDiscoveryListener listener) {
        mInternalLogger.log("setDiscoveryListener");
        mDiscoveryListener = listener;
    }

    public void setDataTransferListener(ClosebyDataTransferListener listener) {
        mDataTransferListener = listener;
    }

    public ClosebyDataTransferListener getDataTransferListener() {
        return mDataTransferListener;
    }

    public void setLogger(ClosebyLogger logger) {
        mLogger = logger;
        generateStateLog();
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    protected BluetoothAdapter getAdapter() {
        return mBluetoothAdapter;
    }

    protected Context getContext() {
        return mContext;
    }

    public ClosebyPeer getPeerByAddress(String address) {
        return mPeers.get(address);
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

    final private ClosebyLogger mInternalLogger = new ClosebyLogger() {
        @Override
        public void log(String logs) {
            Log.d(TAG, logs);
            if (mLogger != null) {
                mLogger.log(DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + " " + logs);
            }
        }
    };

    protected void onPeerDiscovered(ClosebyPeer peer) {
        mPeers.put(peer.getAddress(), peer);
        peer.getDetails();
        //mDiscoveryListener.onPeerFound(peer);
    }

    protected void onPeerDisappeared(String peerAddress) {
        ClosebyPeer peer = mPeers.remove(peerAddress);
        mDiscoveryListener.onPeerDisappeared(peer);
    }

    protected void onPeerDetailsReady(ClosebyPeer peer) {
        mDiscoveryListener.onPeerFound(peer);
    }

    public boolean startAdvertising(ClosebyService service) {
        if (!mBluetoothAdapter.isEnabled()) {
            mInternalLogger.log("Bluetooth is disabled");
            return false;
        }

        if (mGattService == null) {
            mGattService = new ClosebyGattService(this, mInternalLogger);
        }

        if (!mGattService.serve(service)) {
            return false;
        }

        if (mAdvertiser == null) {
            mAdvertiser = new ClosebyAdvertiser(mBluetoothAdapter, mInternalLogger);
        }

        if (!mAdvertiser.start(service)) {
            mInternalLogger.log("Advertise failed.");
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

        if (mAdvertiser != null) {
            mAdvertiser.stop();
        }
    }

    public boolean startDiscovering(UUID service) {
        if (service == null) {
            mInternalLogger.log("null parameter");
            return false;
        }

        if (mDiscoveryListener == null) {
            mInternalLogger.log("please setDiscoveryListener before discovering!");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mInternalLogger.log("Bluetooth is disabled.");
            return false;
        }

        if (mDiscovery == null) {
            mDiscovery = new ClosebyDiscovery(this, mInternalLogger);
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
        return peer.sendData(bytes);
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

