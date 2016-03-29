package net.tplgy.webrtc;

import android.app.Application;

import net.tplgy.closeby.ClosebyPeer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by thomas on 29/03/16.
 */
public class Peers extends Application {
    public ArrayList<ClosebyPeer> mPeers = new ArrayList<>();
    public Map<String, String> mMessages = new HashMap<>();
    public String myself;
}
