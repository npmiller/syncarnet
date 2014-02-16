package fr.insarouen.asi.notesync.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.ProgressDialog;
import android.os.SystemClock;;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.app.Fragment;
import android.net.wifi.p2p.WifiP2pInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.insarouen.asi.notesync.*;
import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.*;

public class NoteSyncBroadcastReceiver extends BroadcastReceiver {

	private WifiP2pManager manager;
	private Channel channel;
	private NoteSync noteSync;
	private PeerList peerList;
	private ProgressDialog progressDialog; 

	public NoteSyncBroadcastReceiver(WifiP2pManager manager, Channel channel, NoteSync noteSync) {
		super();
		this.manager = manager;
		this.channel = channel;
		this.noteSync = noteSync;
		this.peerList = new PeerList(noteSync, manager, channel);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

			// UI update to indicate wifi p2p status.
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				// Wifi Direct mode is enabled
				noteSync.setIsWifiP2pEnabled(true);
			} else {
				noteSync.setIsWifiP2pEnabled(false);
				noteSync.setConnected(false);
				progressDialog = noteSync.getProgressDialog();
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				Toast.makeText(this.noteSync, noteSync.getString(R.string.nowifi),
				       Toast.LENGTH_SHORT).show();

			}
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			// request available peers from the wifi p2p manager. This is an
			// asynchronous call and the calling activity is notified with a
			// callback on PeerListListener.onPeersAvailable()
			if (manager != null) {
				if (!noteSync.isConnected() && noteSync.isWifiP2pEnabled())
					manager.requestPeers(channel, peerList);
			}
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

			if (manager == null) {
				return;
			}

			NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

			if (networkInfo.isConnected()) {

				// we are connected with the other device, request connection
				// info to find group owner IP

				noteSync.setConnected(true);
				Toast.makeText(noteSync, noteSync.getString(R.string.connexionSuccessful), Toast.LENGTH_SHORT).show();
				peerList.setIntent(intent);
				manager.requestConnectionInfo(channel, peerList);
			} else {
				noteSync.setConnected(false);
			}
		}
	}


}

