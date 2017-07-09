package com.balsdon.bleexample;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.HashMap;
import java.util.UUID;

public class BLEPeripheral {

    private String mDeviceId;
    private BLEManager mConnector;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mService = null;
    private HashMap<String, Command<String>> subscriptions;

    public BLEPeripheral(BLEManager connector, String deviceId) {
        mConnector = connector;
        mDeviceId = deviceId;

        mConnector.onDisconnected();
        final BluetoothManager bluetoothManager = (BluetoothManager) mConnector.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mConnector.enableBluetooth();
            return;
        }

        if (!mConnector.checkPermission()) return;

        scanForDevice();
    }

    public void scanForDevice() {
        mConnector.log("BT ENABLED: SCANNING FOR DEVICES");
        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBLEScanner.startScan(mLEScanCallback);
    }

    public void stopScan() {
        mBLEScanner.stopScan(mLEScanCallback);
    }

    private ScanCallback mLEScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            ScanRecord record = result.getScanRecord();
            if (record == null) {
                mConnector.log(String.format("Device [%s] has no scan record", device.getAddress()));
                return;
            }

            String name = record.getDeviceName();
            String UUID = null;

            if (record.getServiceUuids() != null) {
                for (ParcelUuid pId : record.getServiceUuids()) {
                    if (pId.getUuid().toString().equals(mDeviceId)) {
                        UUID = pId.getUuid().toString();
                    }
                }
            }
            if (UUID == null) {
                mConnector.log(String.format("Discovered Device [%s]. Continuing search", name));
                return;
            }
            mConnector.log(String.format("Peripheral [%s] located on Device [%s]. Attempting connection", UUID, name));
            mBluetoothGatt = device.connectGatt(mConnector.getContext(), true, mGattCallback);
            stopScan();
            super.onScanResult(callbackType, result);
        }
    };

    private void closeGatt() {
        mConnector.onDisconnected();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        scanForDevice();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        StringBuilder buffer;
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    mConnector.onConnectionStateChange(newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnector.log("Connected to device GATT. Discovering services");
                        mBluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mConnector.log("Disconnected from GATT server. Continuing scanning");
                        closeGatt();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (mBluetoothGatt.getServices() != null) {
                            for (BluetoothGattService service : mBluetoothGatt.getServices()) {
                                if (service.getUuid().toString().equals(mDeviceId)) {
                                    mService = service;
                                    mConnector.onConnected();
                                    mConnector.log("Service discovered");
                                }
                            }
                        }

                    } else {
                        mConnector.log(String.format("onServicesDiscovered received: [%s]", status));
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mConnector.log(String.format("onCharacteristicRead received: [%s] value: [%s]", characteristic.getUuid().toString(), new String(characteristic.getValue())));
                    } else {
                        mConnector.log(String.format("onCharacteristicRead fail received: [%s]", status));
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mConnector.log(String.format("onCharacteristicWrite received: [%s] value: [%s]", characteristic.getUuid().toString(), new String(characteristic.getValue())));
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    String packet = new String(characteristic.getValue());
                    if (packet.equals(String.valueOf((char)2))) {
                        buffer = new StringBuilder();
                    } else if (packet.equals(String.valueOf((char)3))) {
                        //mConnector.log(String.format("Characteristic received: [%s] Value: [%s]", characteristic.getUuid().toString(), buffer.toString()));
                        if (subscriptions == null || subscriptions.size() == 0) return;

                        Command<String> handler = subscriptions.get(characteristic.getUuid().toString());
                        if (handler != null) handler.execute(new String(buffer.toString()));
                    } else {
                        buffer.append(packet);
                    }
                }
            };

    public BluetoothGattService getService() {
        return mService;
    }

    private BluetoothGattCharacteristic findCharacteristicById(String id) {
        if (mService.getCharacteristics() != null) {
            return mService.getCharacteristic(java.util.UUID.fromString(id));
        }
        return null;
    }

    public void subscribe(String characteristicId, Command<String> handler){
        if (subscriptions == null) subscriptions = new HashMap<>();
        BluetoothGattCharacteristic characteristic = findCharacteristicById(characteristicId);

        if (characteristic == null) {
            mConnector.log("Characteristic does not exist");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

        subscriptions.put(characteristicId, handler);
    }

    public void writeCharacteristic(String characteristicId, String data) {
        BluetoothGattCharacteristic characteristic = findCharacteristicById(characteristicId);
        if (characteristic != null) {
            characteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(characteristic);
            mConnector.log(String.format("Wrote [%s] to [%s]", data, characteristicId));
        } else {
            mConnector.log(String.format("[%s] not found on device", characteristicId));
        }
    }

    public void readCharacteristic(String characteristicId) {
        BluetoothGattCharacteristic characteristic = findCharacteristicById(characteristicId);
        if (characteristic == null) return;
        mBluetoothGatt.readCharacteristic(characteristic);
    }
}
