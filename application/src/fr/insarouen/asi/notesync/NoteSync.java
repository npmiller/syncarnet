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

package fr.insarouen.asi.notesync;

import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.*;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import android.os.Bundle;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import android.widget.Toast;

import java.util.Calendar;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class NoteSync extends Activity implements TaskAddFragment.Callbacks, TaskEditFragment.Callbacks, TaskListFragment.Callbacks {
	private TaskList tasks; 
	private TaskListAdapter adapter;
	private String filename = "tasks";
	private Channel channel;
	public ProgressDialog progressDialog = null;
	public final IntentFilter intentFilter = new IntentFilter();
	private WifiP2pManager manager;
	public PeerListDialog peerListDialog;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Manage SyncService
	public SyncService syncService;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);

		tasks = readTaskList();
		adapter = new TaskListAdapter(this, tasks);

		if(savedInstanceState == null) {
			final ActionBar actionBar = getActionBar();
			actionBar.setHomeButtonEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.container, new TaskListFragment());
			ft.commit();
		}
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// Create a SyncService
		syncService = new SyncService(this, manager, channel, mBluetoothAdapter);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		saveTaskList(tasks);
		if(syncService.receiver != null)
			unregisterReceiver(syncService.receiver);
	}

	public void setTaskList(TaskList taskList) {
		tasks = taskList;
		adapter.setTasks(taskList);
		adapter.notifyDataSetChanged();
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	/* * Fragments callbacks * */

	/* TaskAddFragment */
	/** Switch to the addTask fragment */
	@Override
	public void addTask(Task t) {
		tasks.add(t);
		adapter.notifyDataSetChanged();

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.container, new TaskListFragment());
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
		getFragmentManager().popBackStack();
		ft.commit();
	}

	/* TaskEditFragment */
	/** Get back from EditTask to TaskList fragment and update the TaskList
	  according to the changes.
	  @param Task The new task
	  */
	@Override
	public void replaceTask(Task t) {
		tasks.remove(t); // We need to do this to update the position of the task as well as the projet list
		addTask(t);
		adapter.notifyDataSetChanged();

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.container, new TaskListFragment());
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
		getFragmentManager().popBackStack();
		ft.commit();
	}

	/* TaskListFragment */

	@Override
	public TaskListAdapter getTasksAdapter() {
		return adapter;
	}

	@Override
	public TaskList getTasks() {
		return tasks;
	}

	/** Switch to EditTask view:
	  @param int position of the task to edit in the list
	  */
	@Override
	public void showDetails(int position) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.container, new TaskEditFragment((Task)adapter.getItem(position)));
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.addToBackStack(null);
		ft.commit();
	}

	@Override
	public void removeTask(int position) {
		adapter.removeTask(position);
	}

	/* Menu callbacks */
	@Override
	public void onAddClick() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.container, new TaskAddFragment());
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.addToBackStack(null);
		ft.commit();
	}

	@Override
	public void onClearDeletedClick() {
		tasks.clearDeleted();
		Toast.makeText(this, this.getString(R.string.deletedTaskCleared), Toast.LENGTH_SHORT).show();
	}

	/* Saving and retrieving the local TaskList */
	private void saveTaskList(TaskList tl) {
		try {
			FileOutputStream fos = this.openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(tl);
		} catch (IOException e) {
			Toast toast = Toast.makeText(this,
					this.getString(R.string.nosave),
					Toast.LENGTH_LONG);
			toast.show();
		}
	}

	private TaskList readTaskList() {
		try {
			FileInputStream fis = this.openFileInput(filename);
			ObjectInputStream is = new ObjectInputStream(fis);
			return (TaskList)is.readObject();
		} catch (Exception e) {
			return new TaskList();
		}
	}

	/** Function to use in order to display toasts from other threads */
	public void showToast(final String text) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(NoteSync.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}

	//sync service

	public void onSyncWifiClick() {
		syncService.onSyncWifiClick();
	}

	public void onSyncBTClick() {
		syncService.onSyncBTClick();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		syncService.onBTActivityResult(requestCode, resultCode, data);
	}
}
