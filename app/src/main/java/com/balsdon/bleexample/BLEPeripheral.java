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

    private String deviceId;
    private BLEManager connector;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattService = null;
    private HashMap<String, Command<String>> subscriptions;

    private boolean connected = false;

    public boolean isConnected() {
        return connected;
    }

    public BLEPeripheral(BLEManager connector, String deviceId) {
        this.connector = connector;
        this.deviceId = deviceId;

        this.connector.onDisconnected();
        final BluetoothManager bluetoothManager = (BluetoothManager) this.connector.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            this.connector.enableBluetooth();
            return;
        }

        if (!this.connector.checkPermission()) return;

        scanForDevice();
    }

    public void scanForDevice() {
        connected = false;
        connector.log("BT ENABLED: SCANNING FOR DEVICES");
        connector.reportState(BleManagerStatus.SEARCH_START);
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(mLEScanCallback);
    }

    public void stopScan() {
        connector.reportState(BleManagerStatus.SCAN_CANCEL);
        bluetoothLeScanner.stopScan(mLEScanCallback);
    }

    private ScanCallback mLEScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            ScanRecord record = result.getScanRecord();
            if (record == null) {
                connector.log(String.format("Device [%s] has no scan record", device.getAddress()));
                return;
            }

            String name = record.getDeviceName();
            String UUID = null;

            if (record.getServiceUuids() != null) {
                for (ParcelUuid pId : record.getServiceUuids()) {
                    if (pId.getUuid().toString().equals(deviceId)) {
                        UUID = pId.getUuid().toString();
                    }
                }
            }
            if (UUID == null) {
                if (name != null) connector.log(String.format("Discovered Device [%s]. Continuing search", name));
                return;
            }
            connector.log(String.format("Peripheral [%s] located on Device [%s]. Attempting connection", UUID, name));
            bluetoothGatt = device.connectGatt(connector.getContext(), true, mGattCallback);
            stopScan();
            connector.reportState(BleManagerStatus.DEVICE_FOUND);
            super.onScanResult(callbackType, result);
        }
    };

    private void closeGatt() {
        connector.reportState(BleManagerStatus.DICSONNECT);
        connector.onDisconnected();
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
        scanForDevice();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        StringBuilder buffer;
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    connector.onConnectionStateChange(newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connector.log("Connected to device GATT. Discovering services");
                        connector.reportState(BleManagerStatus.DEVICE_CONNECTED);
                        bluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connector.log("Disconnected from GATT server. Continuing scanning");
                        closeGatt();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (bluetoothGatt.getServices() != null) {
                            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                                if (service.getUuid().toString().equals(deviceId)) {
                                    bluetoothGattService = service;
                                    connector.onConnected();
                                    connector.log("Service discovered");
                                }
                            }
                        }

                    } else {
                        connector.log(String.format("onServicesDiscovered received: [%s]", status));
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        connector.log(String.format("onCharacteristicRead received: [%s] value: [%s]", characteristic.getUuid().toString(), new String(characteristic.getValue())));
                    } else {
                        connector.log(String.format("onCharacteristicRead fail received: [%s]", status));
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        connector.log(String.format("onCharacteristicWrite received: [%s] value: [%s]", characteristic.getUuid().toString(), new String(characteristic.getValue())));
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    String packet = new String(characteristic.getValue());
                    if (packet.equals(String.valueOf((char)2))) {
                        buffer = new StringBuilder();
                    } else if (packet.equals(String.valueOf((char)3))) {
                        if (subscriptions == null || subscriptions.size() == 0) return;

                        Command<String> handler = subscriptions.get(characteristic.getUuid().toString());
                        if (handler != null) handler.execute(new String(buffer.toString()));
                    } else {
                        buffer.append(packet);
                    }
                }
            };

    public BluetoothGattService getService() {
        return bluetoothGattService;
    }

    private BluetoothGattCharacteristic findCharacteristicById(String id) {
        if (bluetoothGattService.getCharacteristics() != null) {
            return bluetoothGattService.getCharacteristic(java.util.UUID.fromString(id));
        }
        return null;
    }

    public void subscribe(String characteristicId, Command<String> handler){
        if (subscriptions == null) subscriptions = new HashMap<>();
        BluetoothGattCharacteristic characteristic = findCharacteristicById(characteristicId);

        if (characteristic == null) {
            connector.log("Characteristic does not exist");
            return;
        }
        connected = true;
        connector.reportState(BleManagerStatus.CHARACTERISTIC_SUBSCRIBED);
        bluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);

        subscriptions.put(characteristicId, handler);
    }

    public void writeCharacteristic(String characteristicId, String data) {
        BluetoothGattCharacteristic characteristic = findCharacteristicById(characteristicId);
        if (characteristic != null) {
            characteristic.setValue(data);
            bluetoothGatt.writeCharacteristic(characteristic);
            connector.log(String.format("Wrote [%s] to [%s]", data, characteristicId));
        } else {
            connector.log(String.format("[%s] not found on device", characteristicId));
        }
    }

    public void readCharacteristic(String characteristicId) {
        BluetoothGattCharacteristic characteristic = findCharacteristicById(characteristicId);
        if (characteristic == null) return;
        bluetoothGatt.readCharacteristic(characteristic);
    }
}
