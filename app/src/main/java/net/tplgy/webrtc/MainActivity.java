package net.tplgy.webrtc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.SyncStateContract;
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
import android.widget.Toast;

import net.tplgy.closeby.Closeby;
import net.tplgy.closeby.ClosebyDiscoveryListener;
import net.tplgy.closeby.ClosebyLogger;
import net.tplgy.closeby.ClosebyService;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static String SERVICE_UUID = "0000546f-0000-1000-8000-00805f9b34fb";
    private final static String NAME_UUID = "11111111-2222-3333-4444-666666666666";
    private ArrayList<String> mDevices;
    private StableArrayAdapter mAdapter;

    Closeby mCloseby;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textview = (TextView) findViewById(R.id.textView);
        textview.setMovementMethod(new ScrollingMovementMethod());

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
        mDevices = new ArrayList<String>();
        mAdapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, mDevices);
        listview.setAdapter(mAdapter);

        final Button central = (Button) findViewById(R.id.button2);
        central.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCloseby.stopDiscovering();
                reset();
                mCloseby.startDiscovering(UUID.fromString(SERVICE_UUID), new ClosebyDiscoveryListener() {
                    @Override

                    public void onNewDevice(String deviceName, String deviceAddress) {
                        mDevices.add(deviceAddress);
                        mAdapter.mIdMap.put(deviceAddress, mDevices.size());
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        final Button peripheral = (Button) findViewById(R.id.button1);
        peripheral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText value = (EditText) findViewById(R.id.editText);
                mCloseby.stopAdvertising();

                ClosebyService s = new ClosebyService(UUID.fromString(SERVICE_UUID));
                s.addProperty(UUID.fromString(NAME_UUID), value.getText().toString().getBytes());
                s.setServiceData("Topology S6".getBytes());
                mCloseby.startAdvertising(s);
            }
        });

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                mCloseby.connect(item);
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

    public void reset() {
        mDevices.clear();
        mAdapter.mIdMap.clear();
        //assert (mAdapter.mIdMap.size() == 0);
        mAdapter.notifyDataSetInvalidated();// .notifyDataSetChanged();
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}
