
package wistron.pushmessage.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BTConnectThread {

    private static final String TAG = "BTConnectThread";
    private static final String NAME = "BTServiceCenter";

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private ArrayList<String> mDeviceAddresses;
    private ArrayList<ConnectedThread> mConnThreads;
    private ArrayList<BluetoothSocket> mSockets;

    // Bluetooth connection status
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    /**
     * Construct
     * @param context 
     * @param handler
     */
    public BTConnectThread(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mDeviceAddresses = new ArrayList<String>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mSockets = new ArrayList<BluetoothSocket>();
    }

    /**
     * Set the current state of the chat connection
     * 
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BTCoreService.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start accept thread
     */
    public synchronized void start() {
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * 
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, device);
        mConnectedThread.start();
        mConnThreads.add(mConnectedThread);
        
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BTCoreService.MESSAGE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(BTCoreService.DEVICE_NAME, device.getName());
        bundle.putString(BTCoreService.DEVICE_ADDR, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        // change status
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(BluetoothDevice device) {
        
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BTCoreService.MESSAGE_DISCONNECT);
        Bundle bundle = new Bundle();
        bundle.putString(BTCoreService.DEVICE_NAME, device.getName());
        bundle.putString(BTCoreService.DEVICE_ADDR, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        setState(STATE_LISTEN);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private class AcceptThread extends Thread {
        BluetoothServerSocket serverSocket = null;

        public AcceptThread() {
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            while (true) {
                try {
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34fb");
                    serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, uuid);
                    socket = serverSocket.accept();
                    serverSocket.close();
                    if (socket != null) {
                        String address = socket.getRemoteDevice().getAddress();
                        mSockets.add(socket);
                        mDeviceAddresses.add(address);
                        connected(socket, socket.getRemoteDevice());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed ", e);
                    break;
                }
            }

        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private BluetoothDevice mDevice;

        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            mmSocket = socket;
            mDevice = device;
            InputStream tmpIn = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(BTCoreService.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "Connection Lost", e);
                    connectionLost(mDevice);
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
