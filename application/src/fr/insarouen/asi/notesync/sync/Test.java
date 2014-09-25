package fr.insarouen.asi.notesync.sync;

import android.util.Log;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.StringBuilder;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Test  implements Serializable {
	private static final String TAG = "NoteSyncService";
	private Integer value;
	private List<String> chain = new ArrayList<String>();

	public Test() {
	}

	public Test(int value) {
		this.value = value;
	}

	public void addString(String string) {
		chain.add(string);
	}

	public String toStringShort() {
		if (chain.isEmpty()) 
			return "empty chain";
		String res = "";
		res += "value = " + value;
		res += ", first string = " + chain.get(0);
		res += ", last string = " + chain.get(chain.size()-1);
		return res;
	}

	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("value = " + value);
		for (int i = 0; i < chain.size(); i++) 
			res.append(", string " + i + " = " + chain.get(i));
		return res.toString();
	}

	public String jsonify() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("value", this.value);
			JSONArray jsonArray = new JSONArray();
			for (int i = 0; i < chain.size(); i++)
				jsonArray.put(chain.get(i));
			jsonObject.put("chain", jsonArray.toString());
			return jsonObject.toString();
		} catch (JSONException e) {
			Log.d(TAG, "Exception while jsonifying");
			return "";
		}
	}

	public void unJsonify(String json) {
		try {
			JSONObject jsonObject = new JSONObject(json);
			this.value = jsonObject.getInt("value");
			Log.d(TAG, "balise");
			JSONArray jsonArray = new JSONArray(jsonObject.getString("chain"));
			for (int i = 0; i < jsonArray.length(); i++)
				chain.add(jsonArray.getString(i));
		} catch (JSONException e) {
			Log.d(TAG, "Exception while unjsonifying");
		}
	}
}
