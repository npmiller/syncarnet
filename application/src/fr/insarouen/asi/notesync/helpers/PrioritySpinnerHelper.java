package fr.insarouen.asi.notesync.helpers;

import fr.insarouen.asi.notesync.tasks.Priority;

import android.widget.Spinner;

public class PrioritySpinnerHelper {
	
	public static void setPriority(Spinner spinner, Priority priority) {
		switch(priority) {
			case HIGH :
				spinner.setSelection(2);
				break;
			case MEDIUM :
				spinner.setSelection(1);
				break;
			case LOW :
				spinner.setSelection(0);
				break;
			default :
				spinner.setSelection(1);
		}
	}

	public static Priority getPriority(int pos) {
		switch(pos) {
			case 0:
				return Priority.LOW;
			case 1:
				return Priority.MEDIUM;
			case 2:
				return Priority.HIGH;
			default:
				return Priority.MEDIUM;
		}
	}
}
