package net.tplgy.closeby;

/**
 * Created by thomas on 21/03/16.
 */
public interface ClosebyPeerListener {
    public void onReady(ClosebyPeer peer);
    public void onFailed(ClosebyPeer peer);
}
