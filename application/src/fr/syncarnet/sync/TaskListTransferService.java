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

package fr.syncarnet.sync;

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

import fr.syncarnet.*;
import fr.syncarnet.tasks.*;
import fr.syncarnet.sync.PeerList.ServiceStatic;

public class TaskListTransferService extends IntentService {
	private String TAG = "SynCarnet";
	private static final int SOCKET_TIMEOUT = 5000;
	private Intent intent;
	private SynCarnet synCarnet;
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
		this.synCarnet = ServiceStatic.getSynCarnet();
		this.host = ServiceStatic.getHost();
		this.isGroupOwner = ServiceStatic.getIsGroupOwner();

		Socket socket = new Socket();
		int port = 8988;
		ServerSocket serverSocket = null;
		Socket client = null;
		TaskListAsync taskListAsync = null;

		if(isGroupOwner) {
			new TaskListAsync(synCarnet).execute();
		} else {
			try {
				TaskList originalTL = new TaskList();
				String originalTLString;
				if (synCarnet.knowPeer(ServiceStatic.getHostId())) {
					originalTLString = synCarnet.getPeer(ServiceStatic.getHostId()).buildDifferentialTaskList(synCarnet.getTasks()).jsonify();
					Log.d(TAG, "Built differential TaskList");
				} else {
					originalTLString = synCarnet.getTasks().jsonify();
					Log.d(TAG, "Device not already known");
				}
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
				outputStream.writeObject(originalTLString);

				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
				String receivedTLString = (String) inputStream.readObject();
				TaskList receivedTL = new TaskList();
				receivedTL.unJsonify(receivedTLString);

				TaskList mergedTL = TaskList.merge(synCarnet.getTasks(), receivedTL);

				synCarnet.runOnUiThread(new SetTaskListRun(synCarnet, mergedTL));

				synCarnet.showToast(synCarnet.getString(R.string.successSync));
				synCarnet.savePeer(ServiceStatic.getHostName(), ServiceStatic.getHostId());
			} catch (IOException e) {
				synCarnet.showToast(synCarnet.getString(R.string.IOException));
				Log.e(TAG,"IOException : "+e.getStackTrace().toString());
			} catch (ClassNotFoundException e) {
				synCarnet.showToast(synCarnet.getString(R.string.ClassNotFoundException));
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
		private SynCarnet synCarnet;
		private String TAG = "SynCarnet";

		/**
		 * @param context
		 * @param statusText
		 */
		public TaskListAsync(SynCarnet synCarnet) {
			this.synCarnet = synCarnet;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				TaskList originalTL = new TaskList();
				//if device is already known => get differential task list
				//device.buildDifferentialTaskList(this.taskList)
				String originalTLString;
				if (synCarnet.knowPeer(ServiceStatic.getHostId())) {
					originalTLString = synCarnet.getPeer(ServiceStatic.getHostId()).buildDifferentialTaskList(synCarnet.getTasks()).jsonify();
					Log.d(TAG, "Built differential TaskList");
				} else {
					originalTLString = synCarnet.getTasks().jsonify();
					Log.d(TAG, "Device not already known");
				}

				ServerSocket serverSocket = new ServerSocket(8988);

				Socket client = serverSocket.accept();
				ObjectInputStream inputStream = new ObjectInputStream(client.getInputStream());
				String receivedTLString = (String) inputStream.readObject();
				TaskList receivedTL = new TaskList();
				receivedTL.unJsonify(receivedTLString);

				TaskList mergedTL = TaskList.merge(synCarnet.getTasks(), receivedTL);

				synCarnet.runOnUiThread(new SetTaskListRun(synCarnet, mergedTL));

				ObjectOutputStream outputStream = new ObjectOutputStream(client.getOutputStream());
				outputStream.writeObject(originalTLString);

				serverSocket.close();
				return "succes";
			}
			catch (IOException e) {
				synCarnet.showToast(synCarnet.getString(R.string.IOException));
				Log.e(TAG,"IOException : "+e.getStackTrace().toString());
				return null;
			}
			catch (ClassNotFoundException e) {
				synCarnet.showToast(synCarnet.getString(R.string.ClassNotFoundException));
				Log.e(TAG,"ClassNotFoundException : "+e.getStackTrace().toString());
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				synCarnet.showToast(synCarnet.getString(R.string.successSync));
				synCarnet.savePeer(ServiceStatic.getHostName(), ServiceStatic.getHostId());
			}
		}

		@Override
		protected void onPreExecute() {
			synCarnet.showToast(synCarnet.getString(R.string.openingSocket));
		}

	}
}

class SetTaskListRun implements Runnable {
	private SynCarnet n;
	private TaskList tl;
	public SetTaskListRun(SynCarnet n, TaskList tl) {
		this.n = n;
		this.tl = tl;
	}

	@Override
	public void run() {
		n.setTaskList(tl);
	}
}
