package com.test.myapplication;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collecting data from WebSocket and stores it on local storage
 * also sending location to server
 */
public class DataCollectingService extends Service implements API.APICallBack
{
	public static final String TAG             = "DataCollectingService";
	public static final int    NOTIFICATION_ID = 0x1000;

	private NotificationManager mNotificationManager;

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

	/**
	 * Connecting to WebSocket
	 */
	private void connectToWS(String login, String password)
	{
		API.getInstance(login, password).connect(this);
	}

	/**
	 * Sending new location to WebSocket
	 */
	private void sendNewLocation(Location location)
	{
		API api = API.getInstance(null, null);

		JSONObject data = new JSONObject();
		try
		{
			data.put("lat", location.getLatitude());
			data.put("lon", location.getLongitude());

			api.send(data.toString());
		}
		catch (JSONException | IllegalStateException e)
		{
			e.printStackTrace();
			if (BuildConfig.DEBUG)
				Log.d(TAG, "sendNewLocation: " + e);
		}
	}

	private void onServiceStart(@Nullable Intent intent)
	{
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMe(TAG + ": started!");

		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener()
		{
			public void onLocationChanged(Location location)
			{
				// Called when a new location is found by the network location provider.
				sendNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		};

		// Register the listener with the Location Manager to receive location updates
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}

		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
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
