package fr.insarouen.asi.notesync.sync;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.StringBuilder;

import java.io.Serializable;

public class Test  implements Serializable {
	private int value;
	private List<String> chaines = new ArrayList<String>();

	public Test() {
	}

	public Test(int value) {
		this.value = value;
	}

	public void addString(String string) {
		chaines.add(string);
	}

	public String toStringShort() {
		if (chaines.isEmpty()) {
			return "empty string";
		}
		String res = "";
		res += "value = " + value;
		res += ", first string = " + chaines.get(0);
		res += ", last string = " + chaines.get(chaines.size()-1);
		return res;
	}

	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("value = " + value);
		for (int i = 0; i == chaines.size() - 1; i++) {
			res.append(", string " + i + " = " + chaines.get(i-1));
		}
		return res.toString();
	}

	public String jsonify() {
		return "";
	}

	public void unJsonify(String json) {
	}
}
