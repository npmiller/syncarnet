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
package fr.insarouen.asi.notesync.tasks;

import android.util.Log;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date; // rightNow = new Date().getTime(); (In Unix Time)

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class DeletedTasks extends ArrayList<UUID> implements Serializable {
	private HashMap<UUID, Long> timestamps = new HashMap<UUID, Long>();
	private static final String TAG = "NoteSync";

	public boolean add(UUID uuid) {
		timestamps.put(uuid, new Long(new Date().getTime()));
		return super.add(uuid);
	}

	public boolean add(UUID uuid, long timestamp) {
		timestamps.put(uuid, new Long(timestamp));
		return super.add(uuid);
	}

	public long getTimestamp(UUID uuid) {
		return (timestamps.get(uuid)).longValue();
	}

	/**
	 * Clear all the deleted tasks older than a given timestamp
	 */
	public void clearDeletedTask(long timestamp) {
		for (UUID uuid : this) {
			if (this.getTimestamp(uuid) < timestamp) {
				this.remove(uuid);
			}
		}
	}

	public String jsonify() {
		JSONObject jsonDT = new JSONObject();
		JSONArray jsonUUIDs = new JSONArray();
		JSONArray jsonTimestamps = new JSONArray();
		JSONObject jsonTTemp;
		try {
			for (int i = 0; i < this.size(); i++)
				jsonUUIDs.put(this.get(i).toString());
			jsonDT.put("uuids", jsonUUIDs.toString());
			Log.d(TAG, "Added uuids to json");

			for (Map.Entry<UUID, Long> entry : timestamps.entrySet()) {
				jsonTTemp = new JSONObject();
				UUID key = entry.getKey();
				jsonTTemp.put("uuid", key.toString());
				Long value = entry.getValue();
				jsonTTemp.put("timestamp", value);
				jsonTimestamps.put(jsonTTemp.toString());
			}
			jsonDT.put("timestamps", jsonTimestamps.toString());
			Log.d(TAG, "Added timestamps to json");

			return jsonDT.toString();
		} catch (JSONException e) {
			Log.e(TAG, "Exception while jsonifying");
			return "";
		}
	}

	public void unJsonify(String json) {
		try {
			JSONObject jsonDT = new JSONObject(json);
			JSONArray jsonUUIDs = new JSONArray(jsonDT.getString("uuids"));
			for (int i = 0; i < this.size(); i++)
				this.add(UUID.fromString(jsonUUIDs.getString(i)));
			Log.d(TAG, "Recreated uuids from json");

			JSONArray jsonTimestamps = new JSONArray(jsonDT.getString("timestamps"));
			JSONObject jsonTTemp;
			for (int i = 0; i < jsonTimestamps.length(); i++) {
				jsonTTemp = new JSONObject(jsonTimestamps.getString(i));
				timestamps.put(UUID.fromString(jsonTTemp.getString("uuid")), jsonTTemp.getLong("timestamp"));
			}
			Log.d(TAG, "Recreated timestamps from json");
		} catch (JSONException e) {
			Log.e(TAG, "Exception while unjsonifying");
		}
	}
} 
