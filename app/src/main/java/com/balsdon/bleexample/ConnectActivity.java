package com.balsdon.bleexample;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.balsdon.bleexample.linux.TerminalCommands;

public class ConnectActivity extends AppCompatActivity implements BLEManager {

    //TODO: THANKS: http://raspberrycan.blogspot.co.uk/

    private static final String TAG = "ConnectActivity";
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private static int REQUEST_ENABLE_BT = 10001;

    private static final String DEVICE_ID = "4afb720a-5214-4337-841b-d5f954214877";
    private static final String CHARACTERISTIC_ID = "8bacc104-15eb-4b37-bea6-0df3ac364199";//"53b3d959-7dd3-4839-94e1-7b0eaea9aac2";

    private enum CurrentHandleStateEnum {
        NONE, WIFI_LIST, WIFI_CONNECT, IP, SSH, VNC, STATS, POWER
    }

    private BLEPeripheral blePeripheral;
    private View loadingView;

    private TextView debugLogText;
    private TextView userLogText;
    private boolean isLoadingShowing = true;
    private CurrentHandleStateEnum state = CurrentHandleStateEnum.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        loadingView = findViewById(R.id.loading_view);
        debugLogText = (TextView) findViewById(R.id.loading_log);
        debugLogText.setMovementMethod(new ScrollingMovementMethod());

        userLogText = (TextView) findViewById(R.id.loading_text);

        blePeripheral = new BLEPeripheral(this, DEVICE_ID);

        findViewById(R.id.action_wifi).findViewById(R.id.action_button).setOnClickListener(wifiListListener);
        findViewById(R.id.action_ip).findViewById(R.id.action_button).setOnClickListener(ipListener);
        findViewById(R.id.action_ssh).findViewById(R.id.action_button).setOnClickListener(sshListener);
        findViewById(R.id.action_vnc).findViewById(R.id.action_button).setOnClickListener(vncListener);
        findViewById(R.id.action_stats).findViewById(R.id.action_button).setOnClickListener(statsListener);
        findViewById(R.id.action_power).findViewById(R.id.action_button).setOnClickListener(powerListener);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setLogo(R.drawable.raspberry_title);
            actionbar.setDisplayUseLogoEnabled(true);
        }
    }

    @Override
    public void log(final String message) {
        if (!BuildConfig.FLAVOR.equals("dev")) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d(TAG, message);
                debugLogText.setText(String.format("%s\n%s", message, debugLogText.getText().toString()));
            }
        });
    }

    private void showState(final int state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                userLogText.setText(state);
            }
        });
    }

    @Override
    public void reportState(BleManagerStatus status) {
        switch (status) {
            case SEARCH_START:
                showState(R.string.status_start);
                break;
            case DEVICE_FOUND:
                showState(R.string.status_found);
                break;
            case DICSONNECT:
                showState(R.string.status_disconnected);
                break;
            case SCAN_CANCEL:
                showState(R.string.status_cancel_search);
                break;
            case DEVICE_CONNECTED:
                showState(R.string.status_connected);
                break;
            case CHARACTERISTIC_SUBSCRIBED:
                showState(R.string.status_subscribed);
                break;
        }
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

    private void animateViewAlpha(final boolean show) {
        if (show && isLoadingShowing) return;
        ValueAnimator animator = ObjectAnimator.ofFloat((show) ? 0.0f : 1.0f, (show) ? 1.0f : 0.0f);
        loadingView.setVisibility(View.VISIBLE);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                loadingView.setAlpha(value);
            }
        });

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) loadingView.setVisibility(View.GONE);
                isLoadingShowing = show;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(1000);
        animator.start();
    }

    private void showButtons() {
        animateViewAlpha(false);
    }

    private void hideButtons() {
        animateViewAlpha(true);
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showButtons();
            }
        });
        blePeripheral.subscribe(CHARACTERISTIC_ID, valueChanged);
    }

    private Command<String> valueChanged = new Command<String>() {
        @Override
        public void execute(final String data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleData(data);
                }
            });
        }
    };

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideButtons();
            }
        });
    }

    @Override
    public boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    blePeripheral.scanForDevice();
                } else {
                    log("Permission denied. App is a brick");
                }
            }
        }
    }
    private void showMessage(String message, @StringRes int actionRes, Command<String> action) {
        showMessage(message, getString(actionRes), action);
    }

    private void showMessage(final String message, String actionString, final Command<String> action) {
        Snackbar snack = Snackbar.make(findViewById(R.id.top), message, BaseTransientBottomBar.LENGTH_LONG);
        snack.setAction(actionString, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.execute(message);
            }
        });
        snack.show();
    }

    private void handleData(String data) {
        switch (state) {
            case WIFI_LIST:
                userSelectWifi(data.replace("SSID: ", "").split("\n"));
                break;
            case WIFI_CONNECT:
                break;
            case IP:
                showMessage(data.replace("\n", ""), android.R.string.copy, copyCommand);
                break;
            case SSH:
                break;
            case VNC:
                break;
            case STATS:
                android.util.Log.d("STATS", String.format("STATS: [%s]", data));
                break;
            case POWER:
                break;
        }
    }

    private View.OnClickListener wifiListListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            state = CurrentHandleStateEnum.WIFI_LIST;
            blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, TerminalCommands.LIST_WIFI);
        }
    };

    private View.OnClickListener ipListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            state = CurrentHandleStateEnum.IP;
            blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, TerminalCommands.GET_IP);
        }
    };

    private View.OnClickListener sshListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private View.OnClickListener vncListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private View.OnClickListener statsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            state = CurrentHandleStateEnum.STATS;
            blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, "");
        }
    };

    private View.OnClickListener powerListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private Command<String> copyCommand = new Command<String>() {
        @Override
        public void execute(String data) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", data);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(ConnectActivity.this, String.format(getString(R.string.copy_toast), data), Toast.LENGTH_SHORT).show();
        }
    };

    private void userSelectWifi(final String[] options) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_wifi_list);

        builder.setSingleChoiceItems(options, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ConnectActivity.this, which + " selected", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ConnectActivity.this, which + " selected", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
