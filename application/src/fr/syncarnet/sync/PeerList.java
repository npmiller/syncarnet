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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.syncarnet.*;
import fr.syncarnet.tasks.*;
import fr.syncarnet.sync.*;

public class PeerList implements PeerListListener, ConnectionInfoListener {
	private List<WifiP2pDevice> peerList;
	private ProgressDialog progressDialog; 
	private SynCarnet synCarnet;
	private WifiP2pManager manager;
	private Channel channel;
	private PeerSelection peerSelection;
	private PeerListDialog peerListDialog;
	private String host;
	private Intent serviceIntent;
	private Intent intent;
	private Boolean display;
	private String TAG = "SynCarnet";

	public PeerList(SynCarnet synCarnet, WifiP2pManager manager, Channel channel, Boolean display) {
		this.peerList = new ArrayList<WifiP2pDevice>();
		this.synCarnet = synCarnet;
		this.manager = manager;
		this.channel = channel;
		this.intent = intent;
		this.peerSelection = new PeerSelection(manager, channel, synCarnet);
		this.peerListDialog = new PeerListDialog(peerList, peerSelection);
		this.display = display;
	}

	public void setIntent(Intent intent) {
		this.intent = intent;
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		if (display) {
			peerList.clear();
			peerList.addAll(peers.getDeviceList());
			if (peerList.size() == 0) {
				Toast.makeText(synCarnet, synCarnet.getString(R.string.noPair), Toast.LENGTH_SHORT).show();
			}
			progressDialog = synCarnet.syncService.getProgressDialog();
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			peerListDialog.setPeerList(peerList);
			synCarnet.syncService.onPeerSelection(peerListDialog);
		}

	}

	@Override 
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		host = info.groupOwnerAddress.getHostAddress();
		serviceIntent = new Intent(synCarnet, TaskListTransferService.class);
		ServiceStatic.set(intent, synCarnet, host, info.isGroupOwner);
		progressDialog = ProgressDialog.show(synCarnet, synCarnet.getString(R.string.backCancel), synCarnet.getString(R.string.syncing), true,
				true, new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {

					}
				});
		synCarnet.syncService.setProgressDialog(progressDialog);
		synCarnet.startService(serviceIntent);
	}

	public static class ServiceStatic {

		public static Intent intent;
		public static SynCarnet synCarnet;
		public static String host;
		public static String hostName;
		public static String hostId;
		public static boolean isGroupOwner;

		public static void set(Intent intent, SynCarnet synCarnet, String host, boolean isGroupOwner) {
			ServiceStatic.intent = intent;
			ServiceStatic.synCarnet = synCarnet;
			ServiceStatic.host = host;
			ServiceStatic.isGroupOwner = isGroupOwner;
		}

		public static void setDevice(String hostName, String hostId) {
			ServiceStatic.hostName = hostName;
			ServiceStatic.hostId = hostId;
		}

		public static Intent getIntent() {
			return ServiceStatic.intent;
		}

		public static SynCarnet getSynCarnet() {
			return ServiceStatic.synCarnet;
		}

		public static String getHost() {
			return ServiceStatic.host;
		}

		public static String getHostName() {
			return ServiceStatic.hostName;
		}

		public static String getHostId() {
			return ServiceStatic.hostId;
		}

		public static boolean getIsGroupOwner() {
			return ServiceStatic.isGroupOwner;
		}


	}

}


