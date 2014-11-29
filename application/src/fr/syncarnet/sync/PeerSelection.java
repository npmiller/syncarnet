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

import fr.syncarnet.*;
import fr.syncarnet.tasks.*;
import fr.syncarnet.sync.*;
import fr.syncarnet.sync.PeerList.ServiceStatic;
import fr.syncarnet.sync.PeerListDialog.OnPeerSelected;

public class PeerSelection implements OnPeerSelected {

	private String TAG = "SynCarnet";
	private ProgressDialog progressDialog = null;
	private WifiP2pManager manager;
	private Channel channel;
	private SynCarnet synCarnet;

	public PeerSelection(WifiP2pManager manager, Channel channel, SynCarnet synCarnet) {
		this.manager = manager;
		this.channel = channel;
		this.synCarnet = synCarnet;
		this.progressDialog = this.synCarnet.syncService.getProgressDialog();
	}

	@Override
	public void onPeerSelected(WifiP2pDevice device) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		synCarnet.syncService.setConnecting(true);
		progressDialog = ProgressDialog.show(synCarnet, synCarnet.getString(R.string.backCancel),
		synCarnet.getString(R.string.connectingTo) + device.deviceAddress, true, true);
		ServiceStatic.setDevice(device.deviceName, device.deviceAddress);
		manager.connect(channel, config, new ActionListener() {
			@Override
			public void onSuccess() {
				// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
			}

			@Override
			public void onFailure(int reason) {
				Toast.makeText(synCarnet, synCarnet.getString(R.string.connectFailed),
				Toast.LENGTH_SHORT).show();
				Log.d(TAG, "Connect failed : "+reason);
				
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

