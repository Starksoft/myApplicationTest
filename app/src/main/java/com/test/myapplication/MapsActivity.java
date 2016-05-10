package com.test.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback
{
	private static final String TAG = "MapsActivity";
	private static GoogleMap mMap;

	private String[] obligatoryPermissions = new String[] {Manifest.permission.ACCESS_FINE_LOCATION};

	private static final int PERMISSIONS_REQUEST_OBLIGATORY = 0;
	private static final int PERMISSIONS_REQUEST_SINGLE     = 1;

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
	{
		switch (requestCode)
		{
			case PERMISSIONS_REQUEST_OBLIGATORY:
			{
				for (int i = 0; i < permissions.length; i++)
				{
					String permission = permissions[i];
					// If request is cancelled, the result arrays are empty.
					if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
					{
						// permission was granted, yay! Do the task you need to do.
					}
					else
					{
						// permission denied, boo! Disable the functionality that depends on this permission.
						boolean quit = false;

						if (TextUtils.equals(permission, Manifest.permission.ACCESS_FINE_LOCATION))
						{
							quit = true;
							Toast.makeText(this, "This application need the location permission to work properly", Toast.LENGTH_SHORT).show();
						}
						//						if (TextUtils.equals(permission, Manifest.permission.GET_ACCOUNTS))
						//						{
						//							quit = true;
						//						}

						if (quit)
							finish();
					}
				}
				break;
			}
			case PERMISSIONS_REQUEST_SINGLE:

				break;
			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	private void checkObligatoryPermissions()
	{
		String request = "";
		for (String permission : obligatoryPermissions)
		{
			int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
			if (permissionCheck != PackageManager.PERMISSION_GRANTED)
				request += permission + ";;;";
		}
		ActivityCompat.requestPermissions(this, request.split(";;;"), PERMISSIONS_REQUEST_OBLIGATORY);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		registerReceiver(localReceiver, new IntentFilter(API.ACTION_DB_NEW_ENTRY));
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(localReceiver);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		checkObligatoryPermissions();

		if (API.isAuthorized())
		{
			setContentView(R.layout.activity_maps);

			// Obtain the SupportMapFragment and get notified when the map is ready to be used.
			SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			mapFragment.getMapAsync(this);
		}
		// Not authorized yet, get us to login mActivity
		else
		{
			//				Toast.makeText(this, "You need to authorize to access this page", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(getBaseContext(), LoginActivity.class);
			startActivity(intent);
			finish();
		}
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap)
	{
		mMap = googleMap;

		mMap.setMyLocationEnabled(true);
		mMap.setTrafficEnabled(true);

		// Restart service need when debug only
		if (BuildConfig.DEBUG)
			startService(new Intent(getBaseContext(), DataCollectingService.class));
	}

	/**
	 * {
	 * "id": 1,
	 * "lat": 55.373703,
	 * "lon": 37.474764
	 * }
	 */
	private MarkerOptions makeMapPoint(JSONObject serverPoint) throws NullPointerException
	{
		if (serverPoint == null)
			throw new NullPointerException();

		double lat = serverPoint.optDouble("lat", -1);
		double lon = serverPoint.optDouble("lon", -1);
		int    id  = serverPoint.optInt("id", -1);

		LatLng position = new LatLng(lat, lon);

		return new MarkerOptions().position(position).title("Point #" + id);
	}

	private void setupPoints(JSONArray array) throws JSONException
	{
		for (int i = 0; i < array.length(); i++)
		{
			final JSONObject point = (JSONObject) array.get(i);

			// Android request to do this on UI thread
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Marker m  = mMap.addMarker(makeMapPoint(point));
						String id = m.getId();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
		}
	}

	BroadcastReceiver localReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent == null)
				return;

			switch (intent.getAction())
			{
				case API.ACTION_DB_NEW_ENTRY:
					if (BuildConfig.DEBUG)
						Log.d(TAG, "onReceive: " + API.ACTION_DB_NEW_ENTRY);

					Bundle extra = intent.getExtras();
					if (extra == null)
						return;

					try
					{
						JSONArray array = new JSONArray(extra.getString(API.EXTRA_DB_NEW_ENTRY));
						setupPoints(array);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					break;
			}

		}
	};
}
