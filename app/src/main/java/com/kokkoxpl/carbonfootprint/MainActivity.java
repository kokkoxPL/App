package com.kokkoxpl.carbonfootprint;

import static android.app.PendingIntent.getActivity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BT";
    SwitchCompat mSwitch;
    Button mButton1;
    Button mButton2;
    Button mButton3;
    TextView mTextView1;
    TextView mTextView2;
    BluetoothService mBluetoothService;
    Set<BluetoothDevice> mPairedDevices;

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
        setContentView(R.layout.activity_main);

        requestBlePermissions(MainActivity.this, 1);

        mSwitch = findViewById(R.id.switch1);
        mButton1 = findViewById(R.id.b1);
        mButton2 = findViewById(R.id.b2);
        mButton3 = findViewById(R.id.b3);
        mTextView1 = findViewById(R.id.editText);
        mTextView2 = findViewById(R.id.resultText);
        mBluetoothService = new BluetoothService(mHandler);


        mSwitch.setOnClickListener(view -> {
            mSwitch.setText(mSwitch.isChecked() ? "Server" : "Client");
        });

        mButton1.setOnClickListener(view -> {
            mPairedDevices = mBluetoothService.findDevices();
            Log.d(TAG, mPairedDevices.toString());
            String str = mPairedDevices.iterator().next().getName() + " : " + mPairedDevices.toString();
            mTextView2.setText(str);
        });

        mButton2.setOnClickListener(view -> {
            BluetoothDevice device;
            if (mSwitch.isChecked()) {
                device = mPairedDevices.iterator().next();
                mBluetoothService.start();
                mTextView2.setText(device.getName() + " (zaczęto słuchać)");
            } else {
                for (BluetoothDevice dev : mPairedDevices) {
                    String deviceAddress = dev.getAddress();
                    Log.d(TAG, deviceAddress);
                    if (deviceAddress.equals("80:54:9C:96:32:44")) {
                        device = dev;
                        mBluetoothService.connect(device, true);
                        mTextView2.setText(device.getName() + " (połaczono)");
                    }
                }
            }
        });
        FragmentActivity activity = this;
        Log.d(TAG, activity.toString());

        mButton3.setOnClickListener(view -> {
            String str = mTextView1.getText().toString();
            mBluetoothService.write(str.getBytes());
        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = MainActivity.this;
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
                    mTextView1.setText(readMessage);
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