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

import fr.syncarnet.tasks.*;
import fr.syncarnet.*;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ActionBar;

import android.os.Bundle;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemLongClickListener;

import java.util.ArrayList;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import android.util.Log;

public class SyncedDevicesFragment extends ListFragment implements OnItemLongClickListener {

	public interface Callbacks {
		public ArrayList<SyncedDevice> getSyncedDevices();
		public void removeSyncedDevice(int pos);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.setListAdapter(
			new ArrayAdapter<SyncedDevice>(
				getActivity(),
				android.R.layout.simple_list_item_1,
				((Callbacks)getActivity()).getSyncedDevices()
			)
		);
		getListView().setOnItemLongClickListener(this);
		setHasOptionsMenu(false); // No options menu 
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// When clicking on an item, ask if the user wants to delete it.
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		adb.setMessage(R.string.unpair);
		adb.setPositiveButton(R.string.yes, new OnUnpairClickListener(position));
		adb.setNegativeButton(R.string.cancel,
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		adb.show();
	}

	private class OnUnpairClickListener implements OnClickListener {
		private int position;

		public OnUnpairClickListener(int position) {
			this.position = position;
		}

		public void onClick(DialogInterface dialog, int id) {
			((Callbacks)getActivity()).removeSyncedDevice(position);
		}
	}

	public boolean onItemLongClick(AdapterView av, View v, int position, long id) {
		//AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		//adb.setMessage(R.string.unpair);
		//adb.setPositiveButton(R.string.yes, new OnListItemLongClickListener(position));
		//adb.setNegativeButton(R.string.cancel,
				//new OnClickListener() {
					//public void onClick(DialogInterface dialog, int id) {
						//dialog.cancel();
					//}
				//});
		//adb.show();
		return true;
	}
}
