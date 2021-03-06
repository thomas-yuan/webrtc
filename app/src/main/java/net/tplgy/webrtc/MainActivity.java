package net.tplgy.webrtc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import net.tplgy.closeby.Closeby;
import net.tplgy.closeby.ClosebyDataTransferListener;
import net.tplgy.closeby.ClosebyDiscoveryListener;
import net.tplgy.closeby.ClosebyLogger;
import net.tplgy.closeby.ClosebyPeer;
import net.tplgy.closeby.ClosebyService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.app.Application;


public class MainActivity extends AppCompatActivity {

    private final static String SERVICE_UUID = "0000546f-0000-1000-8000-00805f9b34fb";
    private final static String EMAIL_UUID = "11111111-2222-3333-4444-666666666666";
    private StableArrayAdapter mAdapter;

    Peers ps;

    Closeby mCloseby;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ps = ((Peers)getApplicationContext());
        final TextView textview = (TextView) findViewById(R.id.textView);
        assert (textview!=null);
        textview.setMovementMethod(new ScrollingMovementMethod());


        final AlertDialog mDialog = new AlertDialog.Builder(this).setTitle("Error")
                .setMessage("Please set username and email address before advertising")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert).create();

        mCloseby = Closeby.getInstance(this);
        if (mCloseby == null) {
            textview.append("\nBluetooth is not supported!!!");
            return;
        }

        mCloseby.setLogger(new ClosebyLogger() {
            @Override
            public void log(final String logs) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textview.append("\n" + logs);
                    }
                });
            }
        });

        final ListView listview = (ListView) findViewById(R.id.listView);
        assert (listview != null);
        ps.mPeers = new ArrayList<>();
        mAdapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, ps.mPeers);
        listview.setAdapter(mAdapter);

        final Button central = (Button) findViewById(R.id.button2);
        assert (central != null);
        mCloseby.setDiscoveryListener(new ClosebyDiscoveryListener() {
            @Override

            public void onPeerFound(final ClosebyPeer peer) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ps.mPeers.add(peer);
                        mAdapter.mIdMap.put(peer, ps.mPeers.size());
                        mAdapter.notifyDataSetChanged();                    }
                });
            }

            public void onPeerDisappeared(final ClosebyPeer peer) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ps.mPeers.remove(peer);
                        mAdapter.mIdMap.remove(peer);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        central.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mPeers.clear();
//                mAdapter.mIdMap.clear();
//                mAdapter.notifyDataSetChanged();
                mCloseby.startDiscovering(UUID.fromString(SERVICE_UUID));
            }
        });

        final Button peripheral = (Button) findViewById(R.id.button1);
        assert (peripheral != null);
        peripheral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final EditText username = (EditText) findViewById(R.id.editText);
                final EditText email = (EditText) findViewById(R.id.editText2);

                if (username.getText().toString().isEmpty() || email.getText().toString().isEmpty()) {
                    mDialog.show();
                    return;
                }
                ps.myself = username.getText().toString();
                mCloseby.stopAdvertising();

                ClosebyService s = new ClosebyService(UUID.fromString(SERVICE_UUID));
                s.addProperty(UUID.fromString(EMAIL_UUID), email.getText().toString().getBytes());
                s.setServiceData(username.getText().toString().getBytes());
                mCloseby.setDataTransferListener(new ClosebyDataTransferListener() {
                    @Override
                    public void onDataReceived(final ClosebyPeer peer, final byte[] data) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textview.append("message received from peer: " + peer.getAddress() + ": " + new String(data));

                                String user = new String(peer.getServiceData());
                                String message = ps.mMessages.get(peer.getAddress());
                                if (null == message) {
                                    ps.mMessages.put(peer.getAddress(), user + ": " + new String(data));
                                } else {
                                    message = message + "\n" + user + ": "+ new String(data);
                                    ps.mMessages.put(peer.getAddress(), message);
                                }

                                Intent intentMain = new Intent(MainActivity.this,
                                        PeerActivity.class);
                                intentMain.putExtra("CLOSEBY_PEER", peer.getAddress());
                                MainActivity.this.startActivity(intentMain);
                            }
                        });
                    }
                });

                mCloseby.startAdvertising(s);
            }
        });

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final ClosebyPeer item = (ClosebyPeer) parent.getItemAtPosition(position);

                Intent intentMain = new Intent(MainActivity.this ,
                        PeerActivity.class);
                intentMain.putExtra("CLOSEBY_PEER", item.getAddress());
                MainActivity.this.startActivity(intentMain);
                Log.i("Content ", " Main layout ");
            }
        });

        if (!mCloseby.isEnabled()) {
            textview.append("\nPlease enable bluetooth.");
        }
    }

    protected void onDestroy() {
        mCloseby.stopAdvertising();
        mCloseby.stopDiscovering();
        super.onDestroy();
    }

    private class StableArrayAdapter extends ArrayAdapter<ClosebyPeer> {

        HashMap<ClosebyPeer, Integer> mIdMap = new HashMap<>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<ClosebyPeer> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            ClosebyPeer item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}
