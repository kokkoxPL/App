package com.kokkoxpl.carbonfootprint;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ClientActivity extends AppCompatActivity {
    private static final String TAG = "BT";
    SwitchCompat mSwitch;
    Button mButtonFindDevices;
    Spinner mBluetoothDeviceSpinner;
    BluetoothService mBluetoothService;
    Set<BluetoothDevice> mBluetoothDeviceSet;


    private static final String[] BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        requestBlePermissions(ClientActivity.this, 1);

        mSwitch = findViewById(R.id.change_side);
        mButtonFindDevices = findViewById(R.id.find_devices);
        mBluetoothDeviceSpinner = findViewById(R.id.device_list);

        mBluetoothService = new BluetoothService(mHandler);
        mBluetoothDeviceSet = new ArraySet<>();


        mSwitch.setOnClickListener(view -> {
            Intent switchActivityIntent = new Intent(this, ServerActivity.class);
            startActivity(switchActivityIntent);
        });

        mButtonFindDevices.setOnClickListener(view -> {
            Set<BluetoothDevice> mBluetoothDeviceSet = mBluetoothService.findDevices();
            if (mBluetoothDeviceSet.isEmpty()) return;

            List<String> list = new ArrayList<>();
            for(BluetoothDevice bluetoothDevice : mBluetoothDeviceSet) {
                list.add(bluetoothDevice.getName());
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
            mBluetoothDeviceSpinner.setAdapter(spinnerAdapter);
        });

//        mButton2.setOnClickListener(view -> {
//            BluetoothDevice device;
//            if (mSwitch.isChecked()) {
//                device = mPairedDevices.iterator().next();
//                mBluetoothService.start();
//                mTextView2.setText(device.getName() + " (zaczęto słuchać)");
//            } else {
//                for (BluetoothDevice dev : mPairedDevices) {
//                    String deviceAddress = dev.getAddress();
//                    Log.d(TAG, deviceAddress);
//                    if (deviceAddress.equals("80:54:9C:96:32:44")) {
//                        device = dev;
//                        mBluetoothService.connect(device, true);
//                        mTextView2.setText(device.getName() + " (połaczono)");
//                    }
//                }
//            }
//        });
//        FragmentActivity activity = this;
//        Log.d(TAG, activity.toString());
//
//        mButton3.setOnClickListener(view -> {
//            String str = mTextView1.getText().toString();
//            mBluetoothService.write(str.getBytes());
//        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = ClientActivity.this;
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Log.d(TAG, "STATE_CONNECTED");
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Log.d(TAG, "STATE_CONNECTING");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            Log.d(TAG, "STATE_LISTEN lub STATE_NONE");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d(TAG, readMessage);
//                    mTextView1.setText(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, msg.getData().getString(Constants.DEVICE_NAME));

                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + msg.getData().getString(Constants.DEVICE_NAME), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
}