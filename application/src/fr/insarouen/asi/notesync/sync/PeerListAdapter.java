package fr.insarouen.asi.notesync.sync;

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.BaseAdapter;

import java.util.List;
import android.net.wifi.p2p.WifiP2pDevice;

import fr.insarouen.asi.notesync.R;

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
