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
		this.progressDialog = this.noteSync.getProgressDialog();
	}

	@Override
	public void onPeerSelected(WifiP2pDevice device) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
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
		noteSync.setConnecting(true);
	}

	@Override
	public void setConnected() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}


}

