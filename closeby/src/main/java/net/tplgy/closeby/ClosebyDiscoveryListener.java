package net.tplgy.closeby;

/**
 * Created by thomas on 2016-03-11.
 */
interface ClosebyDiscoveryListener {
    void onPeerFound(ClosebyPeer peer);
    void onPeerDisappeared(ClosebyPeer peer);
}
