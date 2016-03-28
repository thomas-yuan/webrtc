package net.tplgy.closeby;

/**
 * Created by thomas on 21/03/16.
 */
interface ClosebyPeerListener {
    void onReady(ClosebyPeer peer);
    void onFailed(ClosebyPeer peer);
}
