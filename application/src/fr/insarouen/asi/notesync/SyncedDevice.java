/*
 * Copyright (C) 2014 Nicolas Miller, Florian Paindorge
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

package fr.insarouen.asi.notesync;

import fr.insarouen.asi.notesync.tasks.*;
import java.io.Serializable;
import java.util.Date; // rightNow = new Date().getTime(); (In Unix Time)

public class SyncedDevice {
	// unix timestamp
	private long lastSynchronized;

	public SyncedDevice() {
		// TODO: Add all connections informations Wifi + Bluetooth
	}

	public void updated() {
		this.lastSynchronized = new Date().getTime();
	}

	public long lastSynchronized() {
		return this.lastSynchronized;
	}

	public TaskList buildDifferentialTaskList(TaskList tl) {
		TaskList diffTaskList = new TaskList();
		for (Task t : tl) {
			if (t.getEntry() >= lastSynchronized || t.getModified() >= lastSynchronized) {
				diffTaskList.add(t);
			}
		}
		return diffTaskList;
	}
}
