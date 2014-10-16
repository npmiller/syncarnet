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

package fr.insarouen.asi.notesync.sync;

import fr.insarouen.asi.notesync.*;
import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.DialogInterface;
import android.content.Intent;

import android.content.BroadcastReceiver;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import android.os.Bundle;

import android.util.Log;

import android.widget.Toast;

public class SyncService {
	
	private NoteSync notesync;
	private WifiP2pManager manager;
	public boolean isConnected = false;
	public boolean isConnecting = false;
	private boolean isWifiP2pEnabled;
	public boolean hasPeerList = false;
	private Channel channel;
	public BroadcastReceiver receiver = null;
	private PeerListDialog peerListDialog;
	private SyncBTService mBTService;
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	public SyncService() {
	}

	public SyncService(NoteSync notesync, WifiP2pManager manager, Channel channel, BluetoothAdapter mBluetoothAdapter) {
		this.notesync = notesync;
		this.manager = manager;
		this.channel = channel;
		this.mBluetoothAdapter = mBluetoothAdapter;
	}

	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
		this.isWifiP2pEnabled = isWifiP2pEnabled;
	}

	public boolean isWifiP2pEnabled() {
		return this.isWifiP2pEnabled;
	}

	//wifi part

	public class WifiActionChoiceDialog extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.syncActionChoice)
				.setPositiveButton(R.string.discoverable, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						wifiDiscoverable();
					}
				})
			.setNegativeButton(R.string.searchToSync, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					wifiConnectToPeer();
				}
			});
			// Create the AlertDialog object and return it
			return builder.create();
		}
	}

	public void onSyncWifiClick() {
		DialogFragment choiceFragment = new WifiActionChoiceDialog();
		choiceFragment.show(notesync.getFragmentManager(), "actionSyncChoice");
	}

	public void wifiDiscoverable() {
	}

	public void wifiConnectToPeer() {
		if (!isConnected){
			receiver = new NoteSyncBroadcastReceiver(manager, channel, notesync);
			notesync.registerReceiver(receiver, notesync.intentFilter);
			onInitiateDiscovery();
		} else {
			this.peerListDialog.reconnect(notesync);
		}
	}

	public void onInitiateDiscovery() {
		notesync.progressDialog = ProgressDialog.show(notesync, notesync.getString(R.string.backCancel), notesync.getString(R.string.findingPeers), true,
				true, new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

					}
				});
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {
				Toast.makeText(notesync, notesync.getString(R.string.discoveryInitiated),
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(int reasonCode) {
				Toast.makeText(notesync, notesync.getString(R.string.discoveryFailed),
						Toast.LENGTH_SHORT).show();
				Log.d("NoteSync","Discovery failed : "+reasonCode);
				if (notesync.progressDialog != null && notesync.progressDialog.isShowing()) {
					notesync.progressDialog.dismiss();
				}
			}
		});
	}

	//bt part

	public class BtActionChoiceDialog extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.syncActionChoice)
				.setPositiveButton(R.string.discoverable, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						btDiscoverable();
					}
				})
			.setNegativeButton(R.string.searchToSync, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					btConnectToPeer();
				}
			});
			// Create the AlertDialog object and return it
			return builder.create();
		}
	}

	public void onSyncBTClick() {
		DialogFragment choiceFragment = new BtActionChoiceDialog();
		choiceFragment.show(notesync.getFragmentManager(), "actionSyncChoice");
	}

	public void btDiscoverable() {
	}

	public void btConnectToPeer() {
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			notesync.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			mBTService = new SyncBTService(notesync);
			mBTService.start();
			Intent serverIntent = null;
			serverIntent = new Intent(notesync, fr.insarouen.asi.notesync.sync.DeviceListActivity.class);
			notesync.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					connectDevice(data, true);
				}
				break;
			case REQUEST_CONNECT_DEVICE_INSECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					connectDevice(data, false);
				}
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Initialize the BluetoothChatService to perform bluetooth connections
					mBTService = new SyncBTService(notesync);
					mBTService.start();
					Intent serverIntent = null;
					serverIntent = new Intent(notesync, fr.insarouen.asi.notesync.sync.DeviceListActivity.class);
					notesync.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
				} else {
					// User did not enable Bluetooth or an error occurred
				}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras()
			.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mBTService.connect(device, secure);
	}

	private void ensureDiscoverable() {
		if (mBluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			notesync.startActivity(discoverableIntent);
				}
	}

	//various methods used for verbosity in sync

	public ProgressDialog getProgressDialog() {
		return this.notesync.progressDialog;
	}

	public void setProgressDialog(ProgressDialog progressDialog) {
		this.notesync.progressDialog = progressDialog;
	}

	public void onPeerSelection(PeerListDialog peerListDialog) {
		this.peerListDialog = peerListDialog;
		if (!isConnected && !isConnecting && !peerListDialog.peerListEmpty())
Log.d("NoteSync","balise");
			peerListDialog.show(notesync.getFragmentManager(), "PeerListDialog");
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
		if (isConnected){
			if (peerListDialog != null) {
				peerListDialog.getPeerSelection().setConnected();
				peerListDialog.dismiss();
			}
			if (notesync.progressDialog != null && notesync.progressDialog.isShowing()) {
				notesync.progressDialog.dismiss();
			}
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnecting(boolean isConnecting) {
		this.isConnecting = isConnecting;
	}

	public boolean isConnecting() {
		return this.isConnecting;
	}

}
