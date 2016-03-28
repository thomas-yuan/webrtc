package net.tplgy.closeby;

public interface ClosebyDataTransferListener {

    void onDataReceived(ClosebyPeer peer, byte[] data);

}
