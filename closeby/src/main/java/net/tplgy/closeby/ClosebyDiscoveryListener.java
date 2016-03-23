package net.tplgy.closeby;

/**
 * Created by thomas on 2016-03-11.
 */
public interface ClosebyDiscoveryListener {
    public void onPeerFound(ClosebyPeer peer);
    public void onPeerDisappeared(ClosebyPeer peer);
}
