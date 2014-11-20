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

package fr.insarouen.asi.notesync.sync;

import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.*;
import java.io.Serializable;
import java.util.Date; // rightNow = new Date().getTime(); (In Unix Time)
import java.util.UUID;

public class SyncedDevice implements Serializable {
	private String name;
	private String id;
	// unix timestamp
	private long lastSynchronized;

	public SyncedDevice() {
		this.name = "NULL";
		this.id = "NULL";
		this.lastSynchronized = 0;
	}

	public SyncedDevice(String name, String id, long lastSynchronized) {
		this.name = name;
		this.id = id;
		this.lastSynchronized = lastSynchronized;
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

		// Retrieve deleted tasks that needs to be synchronized
		DeletedTasks deletedTasks = tl.getDeletedTasks();
		DeletedTasks diffDeletedTasks = new DeletedTasks();
		for (UUID uuid : deletedTasks) {
			if (deletedTasks.getTimestamp(uuid) < lastSynchronized) {
				diffDeletedTasks.add(uuid, deletedTasks.getTimestamp(uuid));
			}
		}
		diffTaskList.setDeletedTasks(diffDeletedTasks);

		return diffTaskList;
	}

	public String toString() {
		// The toString is used in the ArrayAdapter
		String lastSync = new Date(lastSynchronized).toLocaleString();
		return name + R.string.lastSync + lastSync;
	}
}
