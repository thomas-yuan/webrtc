package net.tplgy.closeby;

/**
 * Created by thomas on 2016-03-11.
 */
public interface ClosebyDataTransferListener {

    void onDataReceived(ClosebyPeer peer, byte[] data);

}
