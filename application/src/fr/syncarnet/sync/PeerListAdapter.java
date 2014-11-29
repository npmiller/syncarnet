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

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.BaseAdapter;

import java.util.List;
import android.net.wifi.p2p.WifiP2pDevice;

import fr.syncarnet.R;

public class PeerListAdapter extends BaseAdapter {
	private List<WifiP2pDevice> peers;
	private LayoutInflater inflater;

	public PeerListAdapter(Context context, List<WifiP2pDevice> peers) {
		this.inflater = LayoutInflater.from(context);
		this.peers = peers;
	}

	@Override
	public int getCount() {
		return peers.size();
	}

	@Override
	public Object getItem(int position) {
		return peers.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		TextView device;
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.itempeer, null);
			device = (TextView)convertView.findViewById(R.id.name);
			convertView.setTag(device);
		} else {
			device = (TextView) convertView.getTag();
		}
		
		device.setText(peers.get(position).deviceName);
		
		return convertView;
	}
}
