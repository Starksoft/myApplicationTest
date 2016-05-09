package com.test.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{
	private static final String TAG = "MapsActivity";
	private static GoogleMap    mMap;
	static         MapsActivity mActivity;


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
						// permission was granted, yay! Do the contacts-related task you need to do.
					}
					else
					{
						// permission denied, boo! Disable the functionality that depends on this permission.
						boolean quit = false;

						if (TextUtils.equals(permission, Manifest.permission.ACCESS_FINE_LOCATION))
						{
							quit = true;
							//							ApplicationInstance.showErrorToast(this, "Проверка лицензии невозможна без доступа к телефону. Предоставьте разрешение приложению");
						}
						if (TextUtils.equals(permission, Manifest.permission.GET_ACCOUNTS))
						{
							quit = true;
							//							ApplicationInstance.showErrorToast(this, "Проверка лицензии невозможна без доступа к аккаунтам на устройстве. Предоставьте разрешение приложению");
						}

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
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mActivity = this;

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

		Location location = null;
		location = getMyLocation();


		if (location == null)
		{
			Log.d(TAG, "onMapReady: Can`t get lastKnownLocation");
			finish();
			return;
		}

		//		LatLng moscow = new LatLng(55.808024, 37.587059);
		LatLng place = new LatLng(location.getLatitude(), location.getLongitude());
		mMap.moveCamera(CameraUpdateFactory.newLatLng(place));

		API api = API.getInstance(null, null);

		JSONObject data = new JSONObject();
		try
		{
			data.put("lat", location.getLatitude());
			data.put("lon", location.getLongitude());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		api.send(data.toString());
	}

	private Location getMyLocation()
	{
		LocationManager lm         = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location        myLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (myLocation == null)
		{
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			String provider = lm.getBestProvider(criteria, true);
			myLocation = lm.getLastKnownLocation(provider);
		}

		return myLocation;
	}

	/**
	 * {
	 * "id": 1,
	 * "lat": 55.373703,
	 * "lon": 37.474764
	 * }
	 */
	private MarkerOptions makeMapPoint(JSONObject serverPoint)
	{
		if (serverPoint == null)
			return null;

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

	public void onMessage(String s)
	{
		try
		{
			JSONArray array = new JSONArray(s);
			setupPoints(array);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
