package com.example.bluetoothmidilibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class TestClass {
    private Boolean isConnected = false;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;
    private String btDeviceName = "petroochio"; // This is my laptop, change it to be the name of your device! This can be done through unity as well
    private UUID serviceID = UUID.fromString("e0b52790-d10d-11ea-8b6e-0800200c9a66");
    private OutputStream out;
    private InputStream btIn;
    private Thread btInputThread;

    public void SendBTMessage(String msg) {
        if (isConnected) {
            byte[] msgBuffer = msg.getBytes();

            try {
                out.write(msgBuffer);
            } catch (Exception e) {
                Log.d("Unity", "Uh Oh -- message failed to send");
            }
        }
    }

    public void SetDeviceName(String name) {
        btDeviceName = name;
    }

    public void ConnectToBTDrum() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter.isEnabled()) {
            // maybe send info to unity
            Log.d("Unity", "bt adapter working");
            // Find and locate drum in area
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address

                    // If this is the device, go connect
                    if (deviceName.equals(btDeviceName) && !isConnected) {
                        btDevice = btAdapter.getRemoteDevice(deviceHardwareAddress);
//                        btDevice.fetchUuidsWithSdp();
//                        Log.d("Unity", btDevice.getUuids().toString());
                        try {
                            btSocket = btDevice.createRfcommSocketToServiceRecord(serviceID);
                        } catch (Exception e) {
                            Log.d("Unity", "Uh Oh -- couldn't connect to device service");
                        }

                        btAdapter.cancelDiscovery();

                        try {
                            btSocket.connect();
                            isConnected = true;
                            out = btSocket.getOutputStream();
                            btIn = btSocket.getInputStream();
                            SendBTMessage("test boi ");

                            btInputThread = new Thread(new Runnable() {
                                public void run() {
//                                    DataInputStream mmInStream = new DataInputStream(btIn);
                                    BufferedReader buffer=new BufferedReader(new InputStreamReader(btIn));
                                    StringBuilder sb = new StringBuilder();

                                    int r;
                                    try {
                                        while (true) {
                                            r = buffer.read();
                                            if(r != -1) {

                                                char c = (char) r;
                                                if (c == '-') {
                                                    Log.d("Unity", sb.toString());
                                                    UnityPlayer.UnitySendMessage("BluetoothConnector", "PlayNoteFromBluetooth", sb.toString());
                                                    sb.setLength(0);
                                                } else {
                                                    sb.append(c);
                                                }
                                            }
                                        }
                                    } catch (IOException ioe) {}

                                    Log.d("Unity", "Uh Oh -- not reading for some reason");
                                }
                            });
                            btInputThread.start();
                            Log.d("Unity", "Successfully connected");
                        } catch (IOException e) {
                            Log.d("Unity", "Uh Oh -- socket connection failed");
                            try {
                                btSocket.close();
                            } catch (Exception b) {}
                        }
                    }
                }
            }
        } else {
            Log.d("Unity", "Uh Oh -- bt adapter not working");
        }
    }
}
