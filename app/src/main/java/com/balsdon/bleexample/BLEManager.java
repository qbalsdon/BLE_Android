package com.balsdon.bleexample;


import android.content.Context;

public interface BLEManager {
    void log(String message);
    void reportState(BleManagerStatus status);
    void onConnectionStateChange(int newState);
    Context getContext();
    void enableBluetooth();
    void onConnected();
    void onDisconnected();
    boolean checkPermission();
}
