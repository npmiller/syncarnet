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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.ProgressDialog;
import android.os.SystemClock;;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.app.Fragment;
import android.net.wifi.p2p.WifiP2pInfo;
import android.widget.Toast;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.syncarnet.*;
import fr.syncarnet.tasks.*;
import fr.syncarnet.sync.*;
import fr.syncarnet.sync.PeerList.ServiceStatic;

public class SynCarnetBroadcastReceiver extends BroadcastReceiver {

	private WifiP2pManager manager;
	private Channel channel;
	private SynCarnet synCarnet;
	private PeerList peerList;
	private ProgressDialog progressDialog; 
	private Boolean displayPeers;
	private String TAG = "SynCarnet";

	public SynCarnetBroadcastReceiver(WifiP2pManager manager, Channel channel, SynCarnet synCarnet, Boolean displayPeers) {
		super();
		this.manager = manager;
		this.channel = channel;
		this.synCarnet = synCarnet;
		this.peerList = new PeerList(synCarnet, manager, channel, displayPeers);
		this.displayPeers = displayPeers;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

			// UI update to indicate wifi p2p status.
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				// Wifi Direct mode is enabled
				synCarnet.syncService.setIsWifiP2pEnabled(true);
			} else {
				synCarnet.syncService.setIsWifiP2pEnabled(false);
				synCarnet.syncService.setConnected(false);
				progressDialog = synCarnet.syncService.getProgressDialog();
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				Toast.makeText(this.synCarnet, synCarnet.getString(R.string.noWifi),
						Toast.LENGTH_SHORT).show();

			}
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			// request available peers from the wifi p2p manager. This is an
			// asynchronous call and the calling activity is notified with a
			// callback on PeerListListener.onPeersAvailable()
			if (manager != null) {
				if (!synCarnet.syncService.isConnected() && synCarnet.syncService.isWifiP2pEnabled())
					manager.requestPeers(channel, peerList);
			}
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

			if (manager == null) {
				return;
			}

			NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

			if (networkInfo.isConnected()) {
				WifiP2pGroup group = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
				if (group != null) {
					ServiceStatic.setDevice(group.getOwner().deviceName, group.getOwner().deviceAddress);
				}

				// we are connected with the other device, request connection
				// info to find group owner IP

				synCarnet.syncService.setConnected(true);
				progressDialog = synCarnet.syncService.getProgressDialog();
				Toast.makeText(synCarnet, synCarnet.getString(R.string.connexionSuccessful), Toast.LENGTH_SHORT).show();
				peerList.setIntent(intent);
				manager.requestConnectionInfo(channel, peerList);
				if (!displayPeers) {
					Toast.makeText(synCarnet, synCarnet.getString(R.string.syncing), Toast.LENGTH_SHORT).show();
				}
			} else {
				synCarnet.syncService.setConnected(false);
			}
		}
	}


}

