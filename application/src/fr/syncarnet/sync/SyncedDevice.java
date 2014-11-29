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

package fr.syncarnet.sync;

import fr.syncarnet.tasks.*;
import fr.syncarnet.*;
import java.io.Serializable;
import java.util.Date; // rightNow = new Date().getTime(); (In Unix Time)
import java.util.UUID;
import java.text.DateFormat;

public class SyncedDevice implements Serializable {
	private String name;
	private String id;
	private long lastSynchronized; // unix timestamp

	public SyncedDevice(String name, String id) {
		this.name = name;
		this.id = id;
		this.lastSynchronized = new Date().getTime();
	}

	public void updated() {
		this.lastSynchronized = new Date().getTime();
	}

	public long lastSynchronized() {
		return this.lastSynchronized;
	}

	public String getId() {
		return this.id;
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
		String lastSync = DateFormat.getDateInstance().format(new Date(lastSynchronized));
		return name + R.string.lastSync + lastSync;
	}
}
