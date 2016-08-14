package com.balsdon.bleexample;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectActivity extends AppCompatActivity implements BLEManager {

    private static final String TAG = "ConnectActivity";

    private static int REQUEST_ENABLE_BT = 10001;

    private static final String DEVICE_ID = "4afb720a-5214-4337-841b-d5f954214877";
    private static final String CHARACTERISTIC_ID = "53b3d959-7dd3-4839-94e1-7b0eaea9aac2";

    private BLEPeripheral mPeripheral;

    private EditText mInput;
    private TextView mLogger;
    private Button mSendData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mInput = (EditText) findViewById(R.id.input);
        mLogger = (TextView) findViewById(R.id.log);
        mSendData = (Button) findViewById(R.id.send);
        mSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPeripheral.sendData(CHARACTERISTIC_ID, mInput.getText().toString());
                mInput.setText("");
            }
        });

        mPeripheral = new BLEPeripheral(this, DEVICE_ID);
    }

    @Override
    public void log(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConnectActivity.this, message, Toast.LENGTH_SHORT).show();
                android.util.Log.d(TAG, message);
                mLogger.setText(String.format("%s\n%s", message, mLogger.getText().toString()));
            }
        });
    }

    @Override
    public void onConnectionStateChange(int newState) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSendData.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSendData.setEnabled(false);
            }
        });
    }
}
