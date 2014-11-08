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

package fr.insarouen.asi.notesync.sync;

import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;

import fr.insarouen.asi.notesync.*;
import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.sync.PeerList.ServiceStatic;

public class TaskListTransferService extends IntentService {
	private String TAG = "NoteSyncTaskListTransferService";
	private static final int SOCKET_TIMEOUT = 5000;
	private Intent intent;
	private NoteSync noteSync;
	private String host;
	private boolean isGroupOwner;

	public TaskListTransferService() {
		super("TaskListTransferService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		this.intent = ServiceStatic.getIntent();
		this.noteSync = ServiceStatic.getNoteSync();
		this.host = ServiceStatic.getHost();
		this.isGroupOwner = ServiceStatic.getIsGroupOwner();

		Socket socket = new Socket();
		int port = 8988;
		ServerSocket serverSocket = null;
		Socket client = null;
		TaskListAsync taskListAsync = null;

		if(isGroupOwner) {
			new TaskListAsync(noteSync).execute();
		} else {
			try {
				TaskList originalTL = new TaskList();
				String originalTLString = noteSync.getTasks().jsonify();
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
				outputStream.writeObject(originalTLString);

				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
				String receivedTLString = (String) inputStream.readObject();
				TaskList receivedTL = new TaskList();
				receivedTL.unJsonify(receivedTLString);

				TaskList mergedTL = TaskList.merge(noteSync.getTasks(), receivedTL);

				noteSync.runOnUiThread(new SetTaskListRun(noteSync, mergedTL));

				noteSync.showToast(noteSync.getString(R.string.successSync));
			} catch (IOException e) {
				noteSync.showToast(noteSync.getString(R.string.IOException));
				Log.e(TAG,"IOException : "+e.getStackTrace().toString());
			} catch (ClassNotFoundException e) {
				noteSync.showToast(noteSync.getString(R.string.ClassNotFoundException));
				Log.e(TAG,"ClassNotFoundException : "+e.getStackTrace().toString());
			} finally {
				socketClose(socket);
				socketClose(client);
			}
		}

	}

	private void socketClose(Socket s) {
		if(s != null && s.isConnected()) {
			try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class TaskListAsync extends AsyncTask<Void, Void, String> {
		private NoteSync noteSync;
		private String TAG = "NoteSyncTaskListAsync";

		/**
		 * @param context
		 * @param statusText
		 */
		public TaskListAsync(NoteSync noteSync) {
			this.noteSync = noteSync;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				TaskList originalTL = new TaskList();
				String originalTLString = noteSync.getTasks().jsonify();

				ServerSocket serverSocket = new ServerSocket(8988);

				Socket client = serverSocket.accept();
				ObjectInputStream inputStream = new ObjectInputStream(client.getInputStream());
				String receivedTLString = (String) inputStream.readObject();
				TaskList receivedTL = new TaskList();
				receivedTL.unJsonify(receivedTLString);

				TaskList mergedTL = TaskList.merge(noteSync.getTasks(), receivedTL);

				noteSync.runOnUiThread(new SetTaskListRun(noteSync, mergedTL));

				ObjectOutputStream outputStream = new ObjectOutputStream(client.getOutputStream());
				outputStream.writeObject(originalTLString);

				serverSocket.close();
				return "succes";
			}
			catch (IOException e) {
				noteSync.showToast(noteSync.getString(R.string.IOException));
				Log.e(TAG,"IOException : "+e.getStackTrace().toString());
				return null;
			}
			catch (ClassNotFoundException e) {
				noteSync.showToast(noteSync.getString(R.string.ClassNotFoundException));
				Log.e(TAG,"ClassNotFoundException : "+e.getStackTrace().toString());
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				noteSync.showToast(noteSync.getString(R.string.successSync));
			}
		}

		@Override
		protected void onPreExecute() {
			noteSync.showToast(noteSync.getString(R.string.openingSocket));
		}

	}
}

class SetTaskListRun implements Runnable {
	private NoteSync n;
	private TaskList tl;
	public SetTaskListRun(NoteSync n, TaskList tl) {
		this.n = n;
		this.tl = tl;
	}

	@Override
	public void run() {
		n.setTaskList(tl);
	}
}
