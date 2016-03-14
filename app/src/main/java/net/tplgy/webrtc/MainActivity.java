package net.tplgy.webrtc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.tplgy.closeby.Closeby;
import net.tplgy.closeby.ClosebyService;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static String SERVICE_UUID = "B5A7CE02-235C-45FA-A95F-0B5935E04029";
    private final static String NAME_UUID = "0E4E8920-A8B5-4D98-ADEB-63DA6D0BDC2B";
    Closeby mCloseby;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCloseby = Closeby.getInstance(this);
        ClosebyService s = new ClosebyService(UUID.fromString(SERVICE_UUID));
        s.addProperty(UUID.fromString(NAME_UUID), "Topology".getBytes());
        mCloseby.addAdvertiseService(s);
        mCloseby.startAdvertising();
    }
}
