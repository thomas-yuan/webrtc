package net.tplgy.closeby;

/**
 * Created by thomas on 2016-03-11.
 */
public interface ClosebyDataTransferListener {

    void dataReceived(ClosebyPeer peer, Byte[] data);

    void connectionStateChanged(ClosebyPeer peer, Closeby.ConnectionState state);
}
