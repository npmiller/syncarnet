package fr.insarouen.asi.notesync.sync;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.StringBuilder;

import java.io.Serializable;

public class Test  implements Serializable {
	private Integer value;
	private List<String> chaine = new ArrayList<String>();

	public Test() {
	}

	public Test(int value) {
		this.value = value;
	}

	public void addString(String string) {
		chaine.add(string);
	}

	public String toStringShort() {
		if (chaine.isEmpty()) 
			return "empty string";
		String res = "";
		res += "value = " + value;
		res += ", first string = " + chaine.get(0);
		res += ", last string = " + chaine.get(chaine.size()-1);
		return res;
	}

	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("value = " + value);
		for (int i = 0; i < chaine.size(); i++) 
			res.append(", string " + i + " = " + chaine.get(i));
		return res.toString();
	}

	public String jsonifyFalse() {
		StringBuilder res = new StringBuilder();
		res.append(this.value);
		for (int i = 0; i < chaine.size(); i++) 
			res.append("+"+chaine.get(i));
		return res.toString();
	}

	public void unJsonifyFalse(String json) {
		String[] tokens = json.split("+");
		this.value = Integer.parseInt(tokens[0]);
		for (int i = 1; i < tokens.length; i++)
			this.addString(tokens[i]);
	}
}
