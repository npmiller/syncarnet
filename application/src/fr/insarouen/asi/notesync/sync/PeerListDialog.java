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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;

import android.content.DialogInterface;

import android.util.Log;

import java.util.List;

import fr.insarouen.asi.notesync.*;
import fr.insarouen.asi.notesync.R;
import fr.insarouen.asi.notesync.sync.*;

public class PeerListDialog extends DialogFragment {
	private List<WifiP2pDevice> peers;
	private OnPeerSelected callback;
	private PeerListAdapter adapter;
	private Intent serviceIntent;
	private ProgressDialog progressDialog; 
	
	public PeerListDialog(List<WifiP2pDevice> peers, OnPeerSelected callback) {
		this.peers = peers;
		this.callback = callback;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		adapter = new PeerListAdapter(getActivity(), peers);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.choosepeer);
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface Dialog, int which) {
				PeerListDialog.this.callback.onPeerSelected(
					(WifiP2pDevice)PeerListDialog.this.adapter.getItem(which)
					);
				PeerListDialog.this.dismiss();
			}
		});

		return builder.create();
	}

	public void setPeerList(List<WifiP2pDevice> peers) {
		this.peers = peers;
	}

	public OnPeerSelected getPeerSelection() {
		return this.callback;
	}

	public boolean peerListEmpty() {
		return this.peers.isEmpty();
	}

	public void reconnect(NoteSync noteSync) {
		serviceIntent = new Intent(noteSync, TaskListTransferService.class);
		progressDialog = ProgressDialog.show(noteSync, noteSync.getString(R.string.backCancel), noteSync.getString(R.string.syncing), true,
				true, new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {

					}
				});
		noteSync.syncService.setProgressDialog(progressDialog);
		noteSync.startService(serviceIntent);
	}

	public interface OnPeerSelected {
		public void onPeerSelected(WifiP2pDevice device);
		public void setConnected();
	}

}
