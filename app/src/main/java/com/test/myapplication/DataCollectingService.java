package com.test.myapplication;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import org.java_websocket.handshake.ServerHandshake;

/**
 * Collecting data from websocket and stores it on local storage
 */
public class DataCollectingService extends Service implements API.APICallBack
{
	public static final String TAG             = "DataCollectingService";
	public static final int    NOTIFICATION_ID = 0x1000;

	private NotificationManager   mNotificationManager;
	private PowerManager.WakeLock wl;

	public DataCollectingService()
	{ }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		onServiceStart(intent);

		if (intent != null)
		{
			String action = intent.getAction();
			if (TextUtils.equals(action, API.ACTION_CONNECT_TO_WS))
			{
				//				notifyMe(API.ACTION_CONNECT_TO_WS);
				Bundle bundle = intent.getExtras();
				if (bundle != null)
				{
					String login    = bundle.getString("login");
					String password = bundle.getString("password");
					connectToWS(login, password);
				}
			}
		}
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		//		Message msg = mServiceHandler.obtainMessage();
		//		msg.arg1 = startId;
		//		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	private boolean connectToWS(String login, String password)
	{
		API.getInstance(login, password).connect(this);
		return false;
	}

	private void onServiceStart(@Nullable Intent intent)
	{
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notifyMe(TAG + " started!");

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
		wl.acquire();

		//		new Thread(new Runnable()
		//		{
		//			@Override
		//			public void run()
		//			{
		//				try
		//				{
		//					int count = 50;
		//					while (count > 0)
		//					{
		//						count--;
		//						Thread.sleep(1000);
		//						Log.d(TAG, "run: count=" + count);
		//
		//						notifyMe("run: count=" + count);
		//					}
		//
		//					Log.d(TAG, "run: stopSelf()");
		////					stopSelf();
		//				}
		//				catch (InterruptedException e)
		//				{
		//					e.printStackTrace();
		//				}
		//			}
		//		}).start();
		// ... do work...
		//		wl.release();
	}

	private void notifyMe(String text)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.mipmap.ic_launcher);
		builder.setContentTitle(getString(R.string.app_name));
		builder.setContentText(text);
		builder.setOngoing(true);
		builder.setPriority(NotificationCompat.PRIORITY_HIGH);
		builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MapsActivity.class);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		//		stackBuilder.addParentStack(ResultActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(NOTIFICATION_ID, builder.build());
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (wl != null)
			wl.release();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onOpen(ServerHandshake serverHandshake)
	{
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onOpen: " + serverHandshake);

	}

	@Override
	public void onMessage(String s)
	{
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onMessage: " + s);

		//		notifyMe(TAG + ": onMessage");
		if (TextUtils.isEmpty(s))
			return;

		SQLiteDatabase sqLiteDatabase = new DataDB(this).getWritableDatabase();
		try
		{
			sqLiteDatabase = new DataDB(this).getWritableDatabase();

			ContentValues cv = new ContentValues();
			cv.put(DataDB.TIMESTAMP, System.currentTimeMillis());
			cv.put(DataDB.POINTS_JSON, s);

			sqLiteDatabase.insertOrThrow(DataDB.TABLE_NAME, null, cv);

			Intent dbSaveIntent = new Intent(API.ACTION_DB_NEW_ENTRY);
			dbSaveIntent.putExtra(API.EXTRA_DB_NEW_ENTRY, s);
			sendBroadcast(dbSaveIntent);
		}
		catch (SQLException e)
		{
			if (BuildConfig.DEBUG)
				Log.d(TAG, "onMessage: " + e);
		}
		finally
		{
			sqLiteDatabase.close();
		}
	}

	@Override
	public void onClose(int i, String s, boolean b)
	{
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onClose: " + i + s + b);
	}

	@Override
	public void onError(Exception e)
	{
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onError: " + e);
	}
}
