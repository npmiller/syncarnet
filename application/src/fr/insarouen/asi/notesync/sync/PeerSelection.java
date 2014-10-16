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

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.WpsInfo;
import android.util.Log;
import android.widget.Toast;

import fr.insarouen.asi.notesync.*;
import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.*;
import fr.insarouen.asi.notesync.sync.PeerListDialog.OnPeerSelected;

public class PeerSelection implements OnPeerSelected {

	private ProgressDialog progressDialog = null;
	private WifiP2pManager manager;
	private Channel channel;
	private NoteSync noteSync;

	public PeerSelection(WifiP2pManager manager, Channel channel, NoteSync noteSync) {
		this.manager = manager;
		this.channel = channel;
		this.noteSync = noteSync;
		this.progressDialog = this.noteSync.syncService.getProgressDialog();
	}

	@Override
	public void onPeerSelected(WifiP2pDevice device) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		noteSync.syncService.setConnecting(true);
		progressDialog = ProgressDialog.show(noteSync, noteSync.getString(R.string.backCancel),
		noteSync.getString(R.string.connectingTo) + device.deviceAddress, true, true
		);
		manager.connect(channel, config, new ActionListener() {
			@Override
			public void onSuccess() {
				// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
			}

			@Override
			public void onFailure(int reason) {
				Toast.makeText(noteSync, noteSync.getString(R.string.connectFailed),
				Toast.LENGTH_SHORT).show();
				Log.d("NoteSync","Connect failed : "+reason);
				
			}
		});
	}

	@Override
	public void setConnected() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}


}

