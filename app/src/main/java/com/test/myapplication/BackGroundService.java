package com.test.myapplication;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.java_websocket.handshake.ServerHandshake;

public class BackGroundService extends Service implements API.APICallBack
{
	static BackGroundService instance;
	public static final String TAG = "BackGroundService";

	public BackGroundService()
	{
		instance = this;
	}

	PowerManager.WakeLock wl;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
//		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

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

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
		wl.acquire();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int count = 50;
					while (count > 0)
					{
						count--;
						Thread.sleep(1000);
						Log.d(TAG, "run: count=" + count);

						notifyMe("run: count=" + count);
					}

					Log.d(TAG, "run: stopSelf()");
					stopSelf();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}).start();
		// ... do work...
//		wl.release();
	}

	private void notifyMe(String text)
	{
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(text);

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
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(1000, mBuilder.build());
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
