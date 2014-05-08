package fr.insarouen.asi.notesync.sync;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.StringBuilder;

import java.io.Serializable;

public class Test  implements Serializable {
	private int valeur;
	private String chaine = "";

	public Test(int valeur) {
		this.valeur = valeur;
	}

	public void addString(String string) {
		StringBuilder sb = new StringBuilder(this.chaine);
		sb.append(" " + string);
		this.chaine = sb.toString();
	}

	public String toStringShort() {
		return this.valeur + this.chaine.substring(0,5) + this.chaine.substring(this.chaine.length()-5,this.chaine.length());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(this.chaine);
		sb.insert(0,this.valeur + " ");
		return sb.toString();
	}

}
