package fr.insarouen.asi.notesync.sync;

import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.*;
import fr.insarouen.asi.notesync.*;

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
	private static final String TAG = "NoteSyncService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "NoteSyncSecure";
	private static final String NAME_INSECURE = "NoteSyncInsecure";

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
	private NoteSync notesync;
	private TaskList originalTL;
	private byte[] receivedTLBytes;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	/**
	 * Constructor. Prepares a new NoteSync session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public SyncBTService() {
		//this.notesync = notesync;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = null;
	}

	public SyncBTService(NoteSync notesync) {
		this.notesync = notesync;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = null;
		originalTL = notesync.getTasks();
		Log.d(TAG, "taskList retrieved");
	}

	public void setBytes(byte[] buffer) {
		this.receivedTLBytes = buffer;
	}

	public void endSync(boolean server) {
		Log.d(TAG, "last step to sync !");
		if (server) {
			Log.d(TAG, "endSync server");
			//doit fermer la fenêtre de synchro bt
		}
		try {
			TaskList receivedTL = (TaskList) bytesToObject(this.receivedTLBytes);
			Log.e(TAG, "tasklist received : " + receivedTL.toString());
			//notesync.runOnUiThread(new SetTaskListRun(notesync, TaskList.merge(receivedTL, originalTL)));
			//Test ot = (Test) bytesToObject(this.receivedTLBytes);
			//Log.e(TAG, "objet test : " + ot.toString());
			Log.e(TAG, "finally sync !");
		} catch (IOException e) {
				Log.e(TAG, "IOException during bytesToObject", e);
		} catch (ClassNotFoundException e) {
				Log.e(TAG, "ClassNotFoundException during bytesToObject", e);
		}
	}

	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		//mHandler.obtainMessage(NoteSync.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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
		if (D) Log.d(TAG, "start");

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
		if (D) Log.d(TAG, "connect to: " + device);

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
		if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

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

		// Start the thread to manage the connection and perform transmissions
		if (server) {
			mConnectedThreadServer = new ConnectedThreadServer(socket, socketType);
			mConnectedThreadServer.start();
		} else {
			mConnectedThreadClient = new ConnectedThreadClient(socket, socketType);
			mConnectedThreadClient.start();
		}

		// Send the name of the connected device back to the UI Activity
		//Message msg = mHandler.obtainMessage(NoteSync.MESSAGE_DEVICE_NAME);
		//Bundle bundle = new Bundle();
		//bundle.putString(NoteSync.DEVICE_NAME, device.getName());
		//msg.setData(bundle);
		//mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
			}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D) Log.d(TAG, "stop");

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
	 * Write to the ConnectedThread in an unsynchronized manner
	 * @param out The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	//public void write(byte[] out) {
	// Create temporary object
	//ConnectedThread r;
	// Synchronize a copy of the ConnectedThread
	//synchronized (this) {
	//if (mState != STATE_CONNECTED) return;
	//r = mConnectedThread;
	//}
	// Perform the write unsynchronized
	//r.write(out);
	//}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		// Send a failure message back to the Activity
		//Message msg = mHandler.obtainMessage(NoteSync.MESSAGE_TOAST);
		//Bundle bundle = new Bundle();
		//bundle.putString(NoteSync.TOAST, "Unable to connect device");
		//msg.setData(bundle);
		//mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		//NoteSyncService.this.start();
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		// Send a failure message back to the Activity
		//Message msg = mHandler.obtainMessage(NoteSync.MESSAGE_TOAST);
		//Bundle bundle = new Bundle();
		//bundle.putString(NoteSync.TOAST, "Device connection was lost");
		//msg.setData(bundle);
		//mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		//NoteSyncService.this.start();
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
			//notesync.showToast("Accept thread created");
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
			if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

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
					//Toast.makeText(notesync, "secure", Toast.LENGTH_SHORT).show();
				} else {
					tmp = device.createInsecureRfcommSocketToServiceRecord(
							MY_UUID_INSECURE);
					//Toast.makeText(notesync, "insecure", Toast.LENGTH_SHORT).show();
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() " + mSocketType +
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
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThreadServer(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "create ConnectedThreadServer: " + socketType);
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThreadServer");
			byte[] buffer;

			// Keep listening to the InputStream while connected
			//boolean received = false;
			try {
				Log.d(TAG,"now reading");
				DataInputStream d = new DataInputStream(new BufferedInputStream(mmInStream));
				int TLSize = d.readInt();
				Log.e(TAG, "Taille TL : " + TLSize);
				int bytes = 0;
				buffer = new byte[TLSize];
				while (bytes < TLSize) {
					bytes += d.read(buffer,bytes,TLSize-bytes);
					//Log.e(TAG, "bytes=" + bytes);
				}
				Log.e(TAG, "data received");
				Log.e(TAG, bytes + " bytes received");
				SyncBTService.this.setBytes(buffer);
				Log.e(TAG, "buffer set in outer class ");
				//TaskList receivedTL = bytesToTL(buffer);
				//String st = new String(buffer);
				//Log.e(TAG, "TL received : " + st); //test
				//ObjectInputStream ois = new ObjectInputStream(mmInStream);
				//Log.d(TAG,"reading and merging...");
				//notesync.runOnUiThread(new SetTaskListRun(notesync, TaskList.merge((TaskList) ois.readObject(), originalTL)));
				//Log.d(TAG,"task list merged");
				//TaskList receivedTL = (TaskList) ois.readObject();
				//Log.d(TAG,"task list received");
				//TaskList mergedTL = TaskList.merge(originalTL, receivedTL);
				//Log.d(TAG, (String) ois.readObject()+" received");
				//notesync.runOnUiThread(new SetTaskListRun(notesync, mergedTL));
			} catch (IOException e) {
				Log.e(TAG, "disconnected", e);
				connectionLost();
				// Start the service over to restart listening mode
				//NoteSyncService.this.start();
				//break;
			//} catch (ClassNotFoundException e) {
				//Log.d(TAG,"ClassNotFoundException : "+e.getStackTrace().toString());
			}

			try {
				//ObjectOutputStream oos = new ObjectOutputStream(mmOutStream);
				//Log.d(TAG,"writing object...");
				//oos.writeObject(originalTL);
				//Log.d(TAG,"task list sent");
				//oos.writeObject(new String("testServer"));
				this.sleep(01000);
				Test ot = new Test(84);
				ot.addString("chat");
				ot.addString("miaou");
				ot.addString("lait");
				ot.addString("mu");
				ot.addString("dou");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				ot.addString("moustache soyeuse");
				//byte[] bytes = ObjectToBytes((Object) ot);
				byte[] bytes = ObjectToBytes((Object) originalTL);
				int TLSize = bytes.length;
				int BIG_NUM = 100;
				DataOutputStream d = new DataOutputStream(new BufferedOutputStream(mmOutStream,TLSize+4));
				Log.e(TAG, "Taille TL : " + TLSize);
				d.writeInt(TLSize);
				Log.e(TAG, "Taille TL sent");
				this.sleep(01000);
				int i=0;
				for (i=0 ; i<bytes.length ; i+=BIG_NUM) {
					int b = ((i+BIG_NUM) < bytes.length) ? BIG_NUM : bytes.length - i ;
					d.write(bytes,i,b);
					d.flush();
				}
				//Log.e(TAG, "TL sent : " + bytes.hashCode()); //test
				Log.d(TAG,"task list sent");
				this.sleep(10000);
				//d.close();
				//Log.d(TAG,"socket closed");
				SyncBTService.this.endSync(true);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			} catch (InterruptedException e) {
				Log.d(TAG,"InterruptedException : "+e.getStackTrace().toString());
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
				Log.e(TAG, "mConnectedThreadServer closed");
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
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThreadClient(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "create ConnectedThreadClient: " + socketType);
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThreadClient");
			byte[] buffer;

			try {
				Test ot = new Test(66);
				ot.addString("otarie");
				ot.addString("ocean");
				ot.addString("plouf");
				ot.addString("glou");
				ot.addString("honk");
				ot.addString("nageoire");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				ot.addString("huile pas pouih");
				//byte[] bytes = ObjectToBytes((Object) ot);
				byte[] bytes = ObjectToBytes((Object) originalTL);
				int TLSize = bytes.length;
				int BIG_NUM = 100;
				DataOutputStream d = new DataOutputStream(new BufferedOutputStream(mmOutStream,TLSize+4));
				Log.e(TAG, "Taille TL : " + TLSize);
				d.writeInt(TLSize);
				Log.e(TAG, "Taille TL sent");
				int i=0;
				for (i=0 ; i<bytes.length ; i+=BIG_NUM) {
					int b = ((i+BIG_NUM) < bytes.length) ? BIG_NUM : bytes.length - i ;
					d.write(bytes,i,b);
					d.flush();
				}
				//ObjectOutputStream oos = new ObjectOutputStream(mmOutStream);
				//oos.writeObject(originalTL);
				//oos.writeObject(new String("testClient"));
				Log.d(TAG,"task list sent");
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}

			// Keep listening to the InputStream while connected
			//boolean received = false;
			try {
				//ObjectInputStream ois = new ObjectInputStream(mmInStream);
				//Log.d(TAG,"reading and merging...");
				//notesync.runOnUiThread(new SetTaskListRun(notesync, TaskList.merge((TaskList) ois.readObject(), originalTL)));
				//Log.d(TAG,"task list merged");
				//TaskList receivedTL = (TaskList) ois.readObject();
				//Log.d(TAG,"task list received");
				//TaskList mergedTL = TaskList.merge(originalTL, receivedTL);
				//Log.d(TAG, (String) ois.readObject()+" received");
				//notesync.runOnUiThread(new SetTaskListRun(notesync, mergedTL));
				DataInputStream d = new DataInputStream(new BufferedInputStream(mmInStream));
				int TLSize = d.readInt();
				Log.e(TAG, "Taille TL : " + TLSize);
				int bytes = 0;
				buffer = new byte[TLSize];
				while (bytes < TLSize) {
					bytes += d.read(buffer,bytes,TLSize-bytes);
					//Log.e(TAG, "bytes=" + bytes);
				}
				Log.e(TAG, "data received");
				Log.e(TAG, bytes + " bytes received");
				SyncBTService.this.setBytes(buffer);
				Log.e(TAG, "buffer set in outer class ");
				//Log.e(TAG, "TL received : " + buffer.hashCode()); //test
				//String st = new String(buffer);
				//Log.e(TAG, "TL received : " + st); //test
				//TaskList receivedTL = bytesToTL(buffer);
				//this.sleep(06000);
				//d.close();
				//Log.d(TAG,"socket closed");
				SyncBTService.this.endSync(false);
			} catch (IOException e) {
				Log.e(TAG, "disconnected", e);
				connectionLost();
				// Start the service over to restart listening mode
				//NoteSyncService.this.start();
				//break;
			//} catch (InterruptedException e) {
				//Log.d(TAG,"InterruptedException : "+e.getStackTrace().toString());
			//} catch (ClassNotFoundException e) {
				//Log.d(TAG,"ClassNotFoundException : "+e.getStackTrace().toString());
			}

		}

		public void cancel() {
			try {
				mmSocket.close();
				Log.e(TAG, "mConnectedThreadClient closed");
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
