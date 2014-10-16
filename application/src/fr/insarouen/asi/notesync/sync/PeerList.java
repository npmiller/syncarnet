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

import java.util.ArrayList;
import java.util.List;

import fr.insarouen.asi.notesync.*;
import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.*;

public class PeerList implements PeerListListener, ConnectionInfoListener {
	private List<WifiP2pDevice> peerList;
	private ProgressDialog progressDialog; 
	private NoteSync noteSync;
	private WifiP2pManager manager;
	private Channel channel;
	private PeerSelection peerSelection;
	private PeerListDialog peerListDialog;
	private String host;
	private Intent serviceIntent;
	private Intent intent;

	public PeerList(NoteSync noteSync, WifiP2pManager manager, Channel channel) {
		this.peerList = new ArrayList<WifiP2pDevice>();
		this.noteSync = noteSync;
		this.manager = manager;
		this.channel = channel;
		this.intent = intent;
		this.peerSelection = new PeerSelection(manager, channel, noteSync);
		this.peerListDialog = new PeerListDialog(peerList, peerSelection);
	}

	public void setIntent(Intent intent) {
		this.intent = intent;
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		peerList.clear();
		peerList.addAll(peers.getDeviceList());
		if (peerList.size() == 0) {
			Toast.makeText(noteSync, noteSync.getString(R.string.noPair), Toast.LENGTH_SHORT).show();
		}
		progressDialog = noteSync.syncService.getProgressDialog();
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		peerListDialog.setPeerList(peerList);
		noteSync.syncService.onPeerSelection(peerListDialog);

	}

	@Override 
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		host = info.groupOwnerAddress.getHostAddress();
		serviceIntent = new Intent(noteSync, TaskListTransferService.class);
		ServiceStatic.set(intent, noteSync, host, info.isGroupOwner);
		progressDialog = ProgressDialog.show(noteSync, noteSync.getString(R.string.backCancel), noteSync.getString(R.string.syncing), true,
				true, new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {

					}
				});
		noteSync.syncService.setProgressDialog(progressDialog);
		noteSync.startService(serviceIntent);
	}

	public static class ServiceStatic {

		public static Intent intent;
		public static NoteSync noteSync;
		public static String host; 
		public static boolean isGroupOwner;

		public static void set(Intent intent, NoteSync noteSync, String host, boolean isGroupOwner) {
			ServiceStatic.intent = intent;
			ServiceStatic.noteSync = noteSync;
			ServiceStatic.host = host;
			ServiceStatic.isGroupOwner = isGroupOwner;
		}

		public static Intent getIntent() {
			return ServiceStatic.intent;
		}

		public static NoteSync getNoteSync() {
			return ServiceStatic.noteSync;
		}

		public static String getHost() {
			return ServiceStatic.host;
		}

		public static boolean getIsGroupOwner() {
			return ServiceStatic.isGroupOwner;
		}


	}

}


