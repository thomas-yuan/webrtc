package net.tplgy.webrtc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.tplgy.closeby.Closeby;
import net.tplgy.closeby.ClosebyPeer;

import java.util.UUID;

public class PeerActivity extends AppCompatActivity {
    private final static String SERVICE_UUID = "0000546f-0000-1000-8000-00805f9b34fb";

    private final static String EMAIL_UUID = "11111111-2222-3333-4444-666666666666";

    private Closeby mCloseby;
    private ClosebyPeer mPeer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String peerAddress = getIntent().getStringExtra("CLOSEBY_PEER");
        String msg = getIntent().getStringExtra("MESSAGE");
        if (msg != null) {
            final TextView messages = (TextView) findViewById(R.id.textView3);
            assert(messages != null);
            messages.append(msg);
        }


        assert (!peerAddress.isEmpty());
        Log.i("SS", peerAddress);

        final AlertDialog mDialog = new AlertDialog.Builder(this).setTitle("Error")
                .setMessage("Please write something to send")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert).create();


        final EditText message = (EditText) findViewById(R.id.editText3);
        final Button send = (Button) findViewById(R.id.button);
        assert (send != null);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (message.getText().toString().isEmpty()) {
                    mDialog.show();
                    return;
                }

                mCloseby.sendDataToPeer(mPeer, message.getText().toString().getBytes());
            }
        });

        mCloseby = Closeby.getInstance(this);
        ClosebyPeer peer = mCloseby.getPeerByAddress(peerAddress);
        mPeer = peer;
        if (mPeer.getServiceData() != null) {
            setTitle(new String(mPeer.getServiceData()) + " [" + peerAddress + "] MTU " + mPeer.getMTU());
        } else {
            setTitle(new String("[" + peerAddress + "] MTU " + mPeer.getMTU()));
        }
        final TextView textview = (TextView) findViewById(R.id.textView2);
        final byte[] email = mPeer.getProperty(UUID.fromString(EMAIL_UUID));
        textview.setText(new String(email));

        //mCloseby.getProperties(peer, mListener);
    }

}
