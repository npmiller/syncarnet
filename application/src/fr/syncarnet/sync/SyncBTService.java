/*
 * Copyright (C) 2013-14 Nicolas Miller, Florian Paindorge
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fr.syncarnet.sync;

import fr.syncarnet.tasks.*;
import fr.syncarnet.sync.*;
import fr.syncarnet.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.Random;
import java.util.Collections;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class SyncBTService {
	// Debugging
	private static final String TAG = "SynCarnet";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "SynCarnetSecure";
	private static final String NAME_INSECURE = "SynCarnetInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE =
		UUID.fromString("aa87c0d0-afac-11de-8a39-0850200c9a65");
	private static final UUID MY_UUID_INSECURE =
		UUID.fromString("ace255c0-200a-11e0-ac64-0850200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mSecureAcceptThread;
	private AcceptThread mInsecureAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThreadServer mConnectedThreadServer;
	private ConnectedThreadClient mConnectedThreadClient;
	private int mState;
	private SynCarnet synCarnet;
	private TaskList originalTL;
	private byte[] receivedTLBytes;
	private BluetoothDevice device;
	private Boolean isServer;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	/**
	 * Constructor. Prepares a new SynCarnet session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public SyncBTService() {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = null;
	}

	public SyncBTService(SynCarnet synCarnet) {
		this.synCarnet = synCarnet;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = null;
		originalTL = new TaskList();
		originalTL.unJsonify(synCarnet.getTasks().jsonify());
		Log.d(TAG, "TaskList retrieved");
	}

	public void setBytes(byte[] buffer) {
		this.receivedTLBytes = buffer;
	}

	public void endSync(boolean server) {
		Log.d(TAG, "Last step to sync");
		if (server) {
			Log.d(TAG, "EndSync server");
			synCarnet.savePeer(device.getName(), device.getAddress());
		} else {
			try {
				Log.d(TAG, "Rebuilding task list");
				String st = (String) bytesToObject(this.receivedTLBytes);
				Log.d(TAG, "String rebuilt");
				TaskList receivedTL = new TaskList();
				Log.d(TAG, "Unjsonifying");
				receivedTL.unJsonify(st);
				Log.d(TAG, "Task list rebuilt");
				TaskList mergedTL = TaskList.merge(receivedTL, originalTL);
				Log.d(TAG, "Task list merged");
				synCarnet.runOnUiThread(new SetTaskListRun(synCarnet, mergedTL));

				Log.d(TAG, "Sync done");
				synCarnet.showToast(synCarnet.getString(R.string.successSync));
			} catch (IOException e) {
				Log.e(TAG, "IOException during bytesToObject", e);
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "ClassNotFoundException during bytesToObject", e);
			}
		}
	}

	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;
	}

	/**
	 * Return the current connection state. */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume() */
	public synchronized void start() {
		if (D) Log.d(TAG, "Start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThreadServer != null) {mConnectedThreadServer.cancel(); mConnectedThreadServer = null;}
		if (mConnectedThreadClient != null) {mConnectedThreadClient.cancel(); mConnectedThreadClient = null;}

		setState(STATE_LISTEN);

		// Start the thread to listen on a BluetoothServerSocket
		if (mSecureAcceptThread == null) {
			mSecureAcceptThread = new AcceptThread(true);
			mSecureAcceptThread.start();
		}
		if (mInsecureAcceptThread == null) {
			mInsecureAcceptThread = new AcceptThread(false);
			mInsecureAcceptThread.start();
		}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device  The BluetoothDevice to connect
	 * @param secure Socket Security type - Secure (true) , Insecure (false)
	 */
	public synchronized void connect(BluetoothDevice device, boolean secure) {
		if (D) Log.d(TAG, "Connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThreadServer != null) {mConnectedThreadServer.cancel(); mConnectedThreadServer = null;}
		if (mConnectedThreadClient != null) {mConnectedThreadClient.cancel(); mConnectedThreadClient = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device, secure);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice
			device, final String socketType, boolean server) {
		if (D) Log.d(TAG, "Connected, Socket Type:" + socketType);

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThreadServer != null) {mConnectedThreadServer.cancel(); mConnectedThreadServer = null;}
		if (mConnectedThreadClient != null) {mConnectedThreadClient.cancel(); mConnectedThreadClient = null;}

		// Cancel the accept thread because we only want to connect to one device
		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}
		if (mInsecureAcceptThread != null) {
			mInsecureAcceptThread.cancel();
			mInsecureAcceptThread = null;
		}

		this.device = device;

		// Start the thread to manage the connection and perform transmissions
		// chacun a un serveur et un client séparés pour envoyer et recevoir respectivement
		// le serveur fait d'abord le thread server puis le thread client, l'inverse pour le client ; 
		// pour permettre de faire les 2 threads à la suite et essayer d'accélérer
		if (isServer) {
			mConnectedThreadServer = new ConnectedThreadServer(socket, socketType);
			mConnectedThreadServer.start();
		} else {
			mConnectedThreadClient = new ConnectedThreadClient(socket, socketType);
			mConnectedThreadClient.start();
		}

		setState(STATE_CONNECTED);
			}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D) Log.d(TAG, "Stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThreadServer != null) {
			mConnectedThreadServer.cancel();
			mConnectedThreadServer = null;
		}

		if (mConnectedThreadClient != null) {
			mConnectedThreadClient.cancel();
			mConnectedThreadClient = null;
		}
		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}

		if (mInsecureAcceptThread != null) {
			mInsecureAcceptThread.cancel();
			mInsecureAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		//à faire
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		//à faire
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted
	 * (or until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		private String mSocketType;

		public AcceptThread(boolean secure) {
			BluetoothServerSocket tmp = null;
			mSocketType = secure ? "Secure":"Insecure";

			// Create a new listening server socket
			try {
				if (secure) {
					tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
							MY_UUID_SECURE);
				} else {
					tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
							NAME_INSECURE, MY_UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D) Log.d(TAG, "Socket Type: " + mSocketType +
					"BEGIN mAcceptThread" + this);
			setName("AcceptThread" + mSocketType);

			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (SyncBTService.this) {
						switch (mState) {
							case STATE_LISTEN:
							case STATE_CONNECTING:
								// Situation normal. Start the connected thread.
								SyncBTService.this.isServer = true;
								connected(socket, socket.getRemoteDevice(),
										mSocketType, true);
								break;
							case STATE_NONE:
							case STATE_CONNECTED:
								// Either not ready or already connected. Terminate new socket.
								try {
									socket.close();
								} catch (IOException e) {
									Log.e(TAG, "Could not close unwanted socket", e);
								}
								break;
						}
					}
				}
			}
			if (D) Log.d(TAG, "END mAcceptThread, socket Type: " + mSocketType);

		}

		public void cancel() {
			if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
			}
		}
	}


	/**
	 * This thread runs while attempting to make an outgoing connection
	 * with a device. It runs straight through; the connection either
	 * succeeds or fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private String mSocketType;

		public ConnectThread(BluetoothDevice device, boolean secure) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			mSocketType = secure ? "Secure" : "Insecure";

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				if (secure) {
					tmp = device.createRfcommSocketToServiceRecord(
							MY_UUID_SECURE);
				} else {
					tmp = device.createInsecureRfcommSocketToServiceRecord(
							MY_UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.d(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
				SyncBTService.this.isServer = false;
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "Unable to close() " + mSocketType +
							" socket during connection failure", e2);
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (SyncBTService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice, mSocketType, false);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThreadServer extends Thread {
		private final BluetoothSocket mmSocket;
		private final String mmSocketType;
		//private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThreadServer(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "Create ConnectedThreadServer: " + socketType);
			mmSocket = socket;
			mmSocketType = socketType;
			//InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				//tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Temp sockets not created", e);
			}

			//mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.d(TAG, "BEGIN mConnectedThreadServer");
			byte[] buffer;

			try {
				synCarnet.showToast(synCarnet.getString(R.string.connectingTo) + device.getName());
				Log.d(TAG, "Jsonifying");
				String TLString;
				if (synCarnet.knowPeer(device.getAddress())) {
					Log.d(TAG, "Device already known");
					SyncedDevice connectedPeer = synCarnet.getPeer(device.getAddress());
					TLString = connectedPeer.buildDifferentialTaskList(originalTL).jsonify();
					Log.d(TAG, "Built differential TaskList");
				} else {
					Log.d(TAG, "Device not already known");
					TLString = originalTL.jsonify();
				}
				Log.d(TAG, "Jsonifyed");
				Log.d(TAG, "ObjectToBytes");
				byte[] bytes = ObjectToBytes((Object) TLString);
				int TLSize = bytes.length;
				Log.d(TAG, "Server TL size : " + TLSize);
				DataOutputStream d = new DataOutputStream(new BufferedOutputStream(mmOutStream,400));
				d.writeInt(TLSize);
				Log.d(TAG, "TL size sent");
				for (int i=0 ; i<bytes.length ; i++) {
					d.write(bytes[i]);
					d.flush();
					if (i % 1000 == 0) Log.d(TAG,"Sent "+i+" bytes");
				}
				Log.d(TAG,"Task list sent");
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
			if (SyncBTService.this.isServer) {
				mConnectedThreadClient = new ConnectedThreadClient(mmSocket, mmSocketType);
				mConnectedThreadClient.start();
			}

			SyncBTService.this.endSync(true);
		}

		public void cancel() {
			try {
				mmSocket.close();
				Log.d(TAG, "mConnectedThreadServer closed");
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThreadClient extends Thread {
		private final BluetoothSocket mmSocket;
		private final String mmSocketType;
		private final InputStream mmInStream;
		//private final OutputStream mmOutStream;

		public ConnectedThreadClient(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "Create ConnectedThreadClient: " + socketType);
			mmSocket = socket;
			mmSocketType = socketType;
			InputStream tmpIn = null;
			//OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				//tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Temp sockets not created", e);
			}

			mmInStream = tmpIn;
			//mmOutStream = tmpOut;
		}

		public void run() {
			Log.d(TAG, "BEGIN mConnectedThreadClient");
			byte[] buffer;

			// Keep listening to the InputStream while connected
			//boolean received = false;
			try {
				DataInputStream d = new DataInputStream(new BufferedInputStream(mmInStream,400));
				int TLSize = d.readInt();
				Log.d(TAG, "Client TL size : " + TLSize);
				int bytesRead;
				byte[] dataBytes = new byte[TLSize];
				byte[] tmpByte = new byte[1];
				for(bytesRead=0; bytesRead < TLSize; bytesRead++) {
					d.read(tmpByte, 0, 1);
					dataBytes[bytesRead] = tmpByte[0];
					if (bytesRead % 1000 == 0) Log.d(TAG,"Received "+bytesRead+" bytes");
				}
				Log.d(TAG, "Data received");
				Log.d(TAG, bytesRead + " bytes received");
				SyncBTService.this.setBytes(dataBytes);
				Log.d(TAG, "Buffer set in outer class ");
			} catch (IOException e) {
				Log.e(TAG, "Disconnected", e);
				connectionLost();
			}
			if (!SyncBTService.this.isServer) {
				mConnectedThreadServer = new ConnectedThreadServer(mmSocket, mmSocketType);
				mConnectedThreadServer.start();
			}

			SyncBTService.this.endSync(false);

		}

		public void cancel() {
			try {
				mmSocket.close();
				Log.d(TAG, "mConnectedThreadClient closed");
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private static byte[] ObjectToBytes(Object object) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(object);
		return b.toByteArray();
	}

	private static Object bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		ObjectInputStream o = new ObjectInputStream(b);
		return  o.readObject();
	}

}
