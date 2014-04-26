package fr.insarouen.asi.notesync.sync;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import java.io.Serializable;

public class Test extends ArrayList<String> implements Serializable {
	private int valeur;

	public Test(int valeur) {
		this.valeur = valeur;
	}

	public void addString(String string) {
		super.add(string);
	}

	public String toString() {
		String res = "" + this.valeur;
		for (String s : this) 
			res = res + " " + s;
		return res;
	}

}
