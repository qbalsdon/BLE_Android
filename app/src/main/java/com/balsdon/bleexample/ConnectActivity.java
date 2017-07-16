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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.balsdon.bleexample.linux.TerminalCommands;
import com.balsdon.bleexample.linux.TerminalResponse;
import com.balsdon.bleexample.ui.ActionButton;
import com.balsdon.bleexample.ui.StatsDialog;
import com.balsdon.bleexample.ui.WifiInfoDialog;

import java.util.HashMap;
import java.util.Map;

import ru.dimorinny.showcasecard.ShowCaseView;
import ru.dimorinny.showcasecard.position.ViewPosition;
import ru.dimorinny.showcasecard.radius.Radius;

public class ConnectActivity extends AppCompatActivity implements BLEManager {

    //TODO: THANKS: http://raspberrycan.blogspot.co.uk/

    private static final String TAG = "ConnectActivity";
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final int REQUEST_ENABLE_BT = 10001;
    private static final int POWER_OFF = 1;
    private static final int POWER_RESTART = 2;
    private static final int SSH_ENABLE = 3;
    private static final int SSH_DISABLE = 4;
    private static final int SSH_OPEN_APP = 5;
    private static final int VNC_ENABLE = 6;
    private static final int VNC_DISABLE = 7;
    private static final int VNC_OPEN_APP = 8;
    private static final int STATS_TEMPERATURE = 9;
    private static final int STATS_CPU = 10;

    private static final String DEVICE_ID = "4afb720a-5214-4337-841b-d5f954214877";
    private static final String CHARACTERISTIC_ID = "8bacc104-15eb-4b37-bea6-0df3ac364199";

    private enum CurrentHandleStateEnum {
        NONE, WIFI_LIST, WIFI_CONNECT, IP, SSH, VNC, STATS, POWER
    }

    private enum Action {
        NONE, WIFI, IP, SSH, VNC, STATS, POWER
    }

    private BLEPeripheral blePeripheral;
    private View loadingView;

    private TextView debugLogText;
    private TextView userLogText;
    private boolean isLoadingShowing = true;
    private CurrentHandleStateEnum state = CurrentHandleStateEnum.NONE;
    private HashMap<Action, ActionButton> controls;

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

        controls = new HashMap<>();
        findControls();

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setLogo(R.drawable.raspberry_title);
            actionbar.setDisplayUseLogoEnabled(true);
        }

        setupHelpView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (blePeripheral.isConnected()) showAll();
        else hideButtons();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.action_power:
                menu.setHeaderTitle(R.string.menu_power_title);
                menu.add(Menu.NONE, POWER_RESTART, Menu.NONE, R.string.menu_power_restart);
                menu.add(Menu.NONE, POWER_OFF, Menu.NONE, R.string.menu_power_poweroff);
                break;
            case R.id.action_ssh:
                menu.setHeaderTitle(R.string.menu_ssh_title);
                menu.add(Menu.NONE, SSH_ENABLE, Menu.NONE, R.string.menu_ssh_enable);
                menu.add(Menu.NONE, SSH_DISABLE, Menu.NONE, R.string.menu_ssh_disable);
                menu.add(Menu.NONE, SSH_OPEN_APP, Menu.NONE, R.string.menu_ssh_open_app);
                break;
            case R.id.action_vnc:
                menu.setHeaderTitle(R.string.menu_vnc_title);
                menu.add(Menu.NONE, VNC_ENABLE, Menu.NONE, R.string.menu_vnc_enable);
                menu.add(Menu.NONE, VNC_DISABLE, Menu.NONE, R.string.menu_vnc_disable);
                menu.add(Menu.NONE, VNC_OPEN_APP, Menu.NONE, R.string.menu_vnc_open_app);
                break;
            case R.id.action_stats:
                menu.setHeaderTitle(R.string.menu_stats_title);
                menu.add(Menu.NONE, STATS_TEMPERATURE, Menu.NONE, R.string.menu_stats_temperature);
                menu.add(Menu.NONE, STATS_CPU, Menu.NONE, R.string.menu_stats_system_info);
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case POWER_OFF:
                state = CurrentHandleStateEnum.POWER;
                blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, TerminalCommands.SHUTDOWN);
                break;
            case POWER_RESTART:
                state = CurrentHandleStateEnum.POWER;
                blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, TerminalCommands.REBOOT);
                break;
            case SSH_OPEN_APP:
            case SSH_ENABLE:
                runCommand(Action.SSH, CurrentHandleStateEnum.SSH, TerminalCommands.ENABLE_SSH);
                break;
            case SSH_DISABLE:
                runCommand(Action.SSH, CurrentHandleStateEnum.SSH, TerminalCommands.DISABLE_SSH);
                break;
            case VNC_OPEN_APP:
            case VNC_ENABLE:
                runCommand(Action.VNC, CurrentHandleStateEnum.VNC, TerminalCommands.ENABLE_VNC);
                break;
            case VNC_DISABLE:
                runCommand(Action.VNC, CurrentHandleStateEnum.VNC, TerminalCommands.DISABLE_VNC);
                break;
            case STATS_TEMPERATURE:
                runCommand(Action.STATS, CurrentHandleStateEnum.STATS, TerminalCommands.STAT_TEMP);
                break;
            case STATS_CPU:
                runCommand(Action.STATS, CurrentHandleStateEnum.STATS, TerminalCommands.STAT_CPU);
                break;
        }
        return super.onContextItemSelected(item);
    }


    private void findActionButton(Action action, @IdRes int parentId, @StringRes int helpTextReference, View.OnClickListener listener) {
        ActionButton button = (ActionButton) findViewById(parentId);
        button.setImageHelpText(this, helpTextReference);
        button.setOnClickListener(listener);
        controls.put(action, button);
    }

    private void findControls() {
        findActionButton(Action.WIFI, R.id.action_wifi, R.string.help_wifi, wifiListListener);
        findActionButton(Action.IP, R.id.action_ip, R.string.help_ip, ipListener);
        findActionButton(Action.SSH, R.id.action_ssh, R.string.help_ssh, sshListener);
        findActionButton(Action.VNC, R.id.action_vnc, R.string.help_vnc, vncListener);
        findActionButton(Action.STATS, R.id.action_stats, R.string.help_stats, statsListener);
        findActionButton(Action.POWER, R.id.action_power, R.string.help_power, powerListener);
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
        resetControls();
    }

    private void hideButtons() {
        animateViewAlpha(true);
    }

    @Override
    public void onConnected() {
        showAll();
    }

    private void showAll() {
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

    private void showMessage(@StringRes int messageRes) {
        showMessage(getString(messageRes), null, null, null);
    }

    private void showMessage(String message, @StringRes int actionStringRes, Command<String> action) {
        showMessage(message, getString(actionStringRes), message, action);
    }

    private void showMessage(String message, @StringRes int actionStringRes, String data, Command<String> action) {
        showMessage(message, getString(actionStringRes), data, action);
    }

    private void showMessage(String message, String actionString, final String data, final Command<String> action) {
        Snackbar snack = Snackbar.make(findViewById(R.id.top), message, BaseTransientBottomBar.LENGTH_LONG);
        if (action != null && actionString != null && actionString.trim().length() > 0) {
            snack.setAction(actionString, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    action.execute(data);
                }
            });
        }
        snack.show();
    }

    private void handleData(String data) {
        resetControls();
        switch (state) {
            case WIFI_LIST:
                userSelectWifi(data.replace("SSID: ", "").split("\n"));
                break;
            case WIFI_CONNECT: {
                data = data.replace("\n", "");
                TerminalResponse terminalResponse = TerminalResponse.getResponse(data);
                switch (terminalResponse) {
                    case OK:
                        showMessage(R.string.success);
                        break;
                    case CONNECTION_EXISTS:
                        showMessage(R.string.error_wifi_exists);
                        break;
                    case NO_CONNECTION:
                        showMessage(R.string.error_wifi_no_connection);
                        break;
                }
                break;
            }
            case IP:
                showIp(data.replace("\n", "").trim());
                break;
            case SSH: {
                data = data.replace("\n", "");
                TerminalResponse terminalResponse = TerminalResponse.getResponse(data);
                switch (terminalResponse) {
                    case SSH_STARTED:
                        data = data.replace("SSH started: ", "").trim();

                        showMessage(String.format(getString(R.string.success_ssh_message), data), R.string.connect, data, openSshCommand);
                        break;
                    case SSH_STOPPED:
                        showMessage(R.string.success_ssh_stopped_message);
                        break;
                    case SSH_START_FAIL:
                        showMessage(R.string.error_ssh_fail);
                        break;
                }
                break;
            }
            case VNC: {
                data = data.replace("\n", "");
                TerminalResponse terminalResponse = TerminalResponse.getResponse(data);
                switch (terminalResponse) {
                    case VNC_STARTED:
                        data = data.replace("VNC started: ", "");
                        showMessage(String.format(getString(R.string.success_vnc_message), data), R.string.connect, data, openVncCommand);
                        break;
                    case VNC_STOPPED:
                        showMessage(R.string.success_vnc_stopped_message);
                        break;
                    case VNC_START_FAIL:
                        showMessage(R.string.error_ssh_fail);
                        break;
                }
                break;
            }
            case STATS: {
                if (data.toLowerCase().contains("temp")) {
                    StatsDialog.create(this, StatsDialog.Type.TEMPERATURE, String.format("%.2f", Float.parseFloat(data.replaceAll("[^0-9?!\\.]","")))).show();
                } else {
                    StatsDialog.create(this, StatsDialog.Type.CPU_INFO, data).show();
                }
                break;
            }
            case POWER: {
                break;
            }
        }
    }

    private void disableAndAnimate(Action action) {
        for (Map.Entry<Action, ActionButton> entry : controls.entrySet()) {
            if (entry.getKey() == action) continue;
            entry.getValue().setEnabled(false);
        }
        controls.get(action).setLoading(true);
    }

    private void resetControls() {
        for (Map.Entry<Action, ActionButton> entry : controls.entrySet()) {
            entry.getValue().setEnabled(true);
            entry.getValue().setLoading(false);
        }
    }

    private void runCommand(Action action, CurrentHandleStateEnum state, String terminalCommand) {
        disableAndAnimate(action);
        this.state = state;
        blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, terminalCommand);
    }

    private View.OnClickListener wifiListListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            runCommand(Action.WIFI, CurrentHandleStateEnum.WIFI_LIST, TerminalCommands.LIST_WIFI);
        }
    };

    private View.OnClickListener ipListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            runCommand(Action.IP, CurrentHandleStateEnum.IP, TerminalCommands.GET_IP);
        }
    };

    private View.OnClickListener sshListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            registerForContextMenu(controls.get(Action.SSH));
            openContextMenu(controls.get(Action.SSH));
        }
    };

    private View.OnClickListener vncListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            registerForContextMenu(controls.get(Action.VNC));
            openContextMenu(controls.get(Action.VNC));

        }
    };

    private View.OnClickListener statsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            registerForContextMenu(controls.get(Action.STATS));
            openContextMenu(controls.get(Action.STATS));
        }
    };

    private View.OnClickListener powerListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            registerForContextMenu(controls.get(Action.POWER));
            openContextMenu(controls.get(Action.POWER));
        }
    };

    private void showIp(final String ip) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.dialog_ip_title);
        dialogBuilder.setMessage(ip);
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogBuilder.setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                copyCommand.execute(ip);
            }
        });
        dialogBuilder.create().show();
    }

    private Command<String> copyCommand = new Command<String>() {
        @Override
        public void execute(String data) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", data);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(ConnectActivity.this, String.format(getString(R.string.copy_toast), data), Toast.LENGTH_SHORT).show();
        }
    };

    private Command<String> wifiCommand = new Command<String>() {
        @Override
        public void execute(String data) {
            state = CurrentHandleStateEnum.WIFI_CONNECT;
            blePeripheral.writeCharacteristic(CHARACTERISTIC_ID, data);
        }
    };

    private Command<String> openSshCommand = new Command<String>() {
        @Override
        public void execute(String ip) {
            getSshUserName(ip);
        }
    };

    private void openSsh(String user, String ip) {
        ip = ip.trim();
        Uri ssh = Uri.parse(String.format("ssh://%1$s/#%1$s", String.format("%s@%s", user, ip)));
        Intent intent = new Intent(Intent.ACTION_VIEW, ssh);

        String title = getResources().getString(R.string.ssh_chooser_title);

        Intent chooser = Intent.createChooser(intent, title);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            showMessage(getString(R.string.error_no_ssh_client), R.string.open_app_store, new Command<String>() {
                @Override
                public void execute(String data) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.connectbot")));
                }
            });
        }
    }

    private Command<String> openVncCommand = new Command<String>() {
        @Override
        public void execute(String data) {
            copyCommand.execute(data);
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.realvnc.viewer.android");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                showMessage(getString(R.string.error_no_vnc_client), R.string.open_app_store, new Command<String>() {
                    @Override
                    public void execute(String data) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.realvnc.viewer.android")));
                    }
                });
            }
        }
    };

    private void userSelectWifi(final String[] options) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_wifi_list);

        builder.setSingleChoiceItems(options, 0, null);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                which = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                WifiInfoDialog.create(ConnectActivity.this, options[which], wifiCommand).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void getSshUserName(final String ipAddress) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        final EditText usernameText = new EditText(this);
        usernameText.setHint(R.string.dialog_ssh_password_hint);
        usernameText.setText(R.string.dialog_ssh_password_default);
        inputLayout.addView(usernameText);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_ssh_user_title)
                .setView(inputLayout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openSsh(usernameText.getText().toString(), ipAddress);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void setupHelpView() {
        final ImageView imageView = (ImageView)findViewById(R.id.loading_help);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ShowCaseView.Builder(ConnectActivity.this)
                        .withTypedPosition(new ViewPosition(imageView))
                        .withTypedRadius(new Radius(186F))
                        .withContent(
                                getString(R.string.text_help)
                        )
                        .build()
                        .show(ConnectActivity.this);
            }
        });
    }
}
