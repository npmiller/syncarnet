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

import fr.syncarnet.*;
import fr.syncarnet.tasks.*;
import fr.syncarnet.sync.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;

import android.net.wifi.WifiManager;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import android.os.Bundle;

import android.provider.Settings;

import android.util.Log;

import android.widget.Toast;

public class SyncService {

	private String TAG = "SynCarnet";
	private SynCarnet synCarnet;
	private WifiP2pManager manager;
	private boolean isConnected = false;
	private boolean isConnecting = false;
	private boolean isWifiP2pEnabled;
	private Channel channel;
	private SyncBTService mBTService;
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	public SyncService() {
	}

	public SyncService(SynCarnet synCarnet, WifiP2pManager manager, Channel channel, BluetoothAdapter mBluetoothAdapter) {
		this.synCarnet = synCarnet;
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

	protected void enableWifiDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(synCarnet);
		builder.setMessage(synCarnet.getString(R.string.needWifi))
			.setTitle(synCarnet.getString(R.string.noWifi))
			.setCancelable(false)
			.setPositiveButton(synCarnet.getString(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
							synCarnet.startActivity(i);
						}
			}
			)
			.setNegativeButton(synCarnet.getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
			}
			);
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void onSyncWifiClick() {
		WifiManager wifi = (WifiManager) synCarnet.getSystemService(Context.WIFI_SERVICE);
		synCarnet.showToast(synCarnet.getString(R.string.reconnectInfo), false);
		if (!wifi.isWifiEnabled()) {
			enableWifiDialog();
		} else {
			DialogFragment choiceFragment = new WifiActionChoiceDialog();
			choiceFragment.show(synCarnet.getFragmentManager(), "actionSyncChoice");
		}
	}

	public void wifiDiscoverable() {
		if (!isConnected){
			synCarnet.receiver = new SynCarnetBroadcastReceiver(manager, channel, synCarnet, false);
			synCarnet.registerReceiver(synCarnet.receiver, synCarnet.intentFilter);
			manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {
				Toast.makeText(synCarnet, synCarnet.getString(R.string.wifiDiscoverable),
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(int reasonCode) {
				Toast.makeText(synCarnet, synCarnet.getString(R.string.discoveryFailed),
						Toast.LENGTH_SHORT).show();
				Log.d("SynCarnet","Discovery failed : "+reasonCode);
				if (synCarnet.progressDialog != null && synCarnet.progressDialog.isShowing()) {
					synCarnet.progressDialog.dismiss();
				}
			}
		});
		} else {
			Toast.makeText(this.synCarnet, synCarnet.getString(R.string.peeredWifi), Toast.LENGTH_SHORT).show();
		}
	}

	public void wifiConnectToPeer() {
		if (!isConnected){
			synCarnet.receiver = new SynCarnetBroadcastReceiver(manager, channel, synCarnet, true);
			synCarnet.registerReceiver(synCarnet.receiver, synCarnet.intentFilter);
			onInitiateDiscovery();
		} else {
			synCarnet.peerListDialog.reconnect(synCarnet);
		}
	}

	public void onInitiateDiscovery() {
		synCarnet.progressDialog = ProgressDialog.show(synCarnet, synCarnet.getString(R.string.backCancel), synCarnet.getString(R.string.findingPeers), true,
				true, new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

					}
				});
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {
				Toast.makeText(synCarnet, synCarnet.getString(R.string.discoveryInitiated),
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(int reasonCode) {
				Toast.makeText(synCarnet, synCarnet.getString(R.string.discoveryFailed),
						Toast.LENGTH_SHORT).show();
				Log.d("SynCarnet","Discovery failed : "+reasonCode);
				if (synCarnet.progressDialog != null && synCarnet.progressDialog.isShowing()) {
					synCarnet.progressDialog.dismiss();
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
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			synCarnet.startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
		} else {
			DialogFragment choiceFragment = new BtActionChoiceDialog();
			choiceFragment.show(synCarnet.getFragmentManager(), "actionSyncChoice");
		}
	}

	public void btDiscoverable() {
		ensureDiscoverable();
		mBTService = new SyncBTService(synCarnet);
		mBTService.start();
	}

	public void btConnectToPeer() {
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		mBTService = new SyncBTService(synCarnet);
		mBTService.start();
		Intent serverIntent = null;
		serverIntent = new Intent(synCarnet, fr.syncarnet.sync.DeviceListActivity.class);
		synCarnet.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
	}

	public void onBTActivityResult(int requestCode, int resultCode, Intent data) {
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
					DialogFragment choiceFragment = new BtActionChoiceDialog();
					choiceFragment.show(synCarnet.getFragmentManager(), "actionSyncChoice");
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
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			synCarnet.startActivity(discoverableIntent);
		}
	}

	//various methods used for verbosity in sync

	public ProgressDialog getProgressDialog() {
		return this.synCarnet.progressDialog;
	}

	public void setProgressDialog(ProgressDialog progressDialog) {
		this.synCarnet.progressDialog = progressDialog;
	}

	public void onPeerSelection(PeerListDialog peerListDialog) {
		if (synCarnet.peerListDialog == null) {
			synCarnet.peerListDialog = peerListDialog;
			if (!isConnected && !isConnecting && !synCarnet.peerListDialog.peerListEmpty())
				synCarnet.peerListDialog.show(synCarnet.getFragmentManager(), "PeerListDialog");
		}
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
		if (isConnected){
			if (synCarnet.peerListDialog != null) {
				synCarnet.peerListDialog.getPeerSelection().setConnected();
				synCarnet.peerListDialog.dismiss();
			}
			if (synCarnet.progressDialog != null && synCarnet.progressDialog.isShowing()) {
				synCarnet.progressDialog.dismiss();
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
