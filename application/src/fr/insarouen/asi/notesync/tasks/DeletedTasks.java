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
package fr.insarouen.asi.notesync.tasks;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date; // rightNow = new Date().getTime(); (In Unix Time)

import java.io.Serializable;

public class DeletedTasks extends ArrayList<UUID> implements Serializable {
	private HashMap<UUID, Long> timestamps = new HashMap<UUID, Long>();

	public boolean add(UUID uuid) {
		timestamps.put(uuid, new Long(new Date().getTime()));
		return super.add(uuid);
	}

	public boolean add(UUID uuid, long timestamp) {
		timestamps.put(uuid, new Long(timestamp));
		return super.add(uuid);
	}

	public long getTimestamp(UUID uuid) {
		return (timestamps.get(uuid)).longValue();
	}

	/**
	 * Clear all the deleted tasks older than a given timestamp
	 */
	public void clearDeletedTask(long timestamp) {
		for (UUID uuid : this) {
			if (this.getTimestamp(uuid) < timestamp) {
				this.remove(uuid);
			}
		}
	}
} 
