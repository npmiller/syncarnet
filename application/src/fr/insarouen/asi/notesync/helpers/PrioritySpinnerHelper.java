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
