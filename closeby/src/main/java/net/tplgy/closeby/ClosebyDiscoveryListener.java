package net.tplgy.closeby;

public interface ClosebyDiscoveryListener {
    void onPeerFound(ClosebyPeer peer);
    void onPeerDisappeared(ClosebyPeer peer);
}
