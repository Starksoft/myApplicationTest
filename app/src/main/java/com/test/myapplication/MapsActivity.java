package com.test.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, API.APICallBack
{
	private static GoogleMap mMap;
	static MapsActivity activity;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		startService(new Intent(getBaseContext(), BackGroundService.class));
		activity = this;

		if (API.isAuthorized())
		{
			setContentView(R.layout.activity_maps);

			// Obtain the SupportMapFragment and get notified when the map is ready to be used.
			SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			mapFragment.getMapAsync(this);
		}
		// Not authorized yet, get us to login activity
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

		LatLng moscow = new LatLng(55.808024, 37.587059);

		mMap.addMarker(new MarkerOptions().position(moscow).title("Place of development :)"));
		mMap.moveCamera(CameraUpdateFactory.newLatLng(moscow));

		API api = API.getInstance(null, null);

		JSONObject data = new JSONObject();
		try
		{
			data.put("lat", 55.808024);
			data.put("lon", 37.587059);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		api.send(data.toString());
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
		int id = serverPoint.optInt("id", -1);

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
					Marker m = mMap.addMarker(makeMapPoint(point));
					String id = m.getId();
				}
			});
		}
	}


	@Override
	public void onOpen(ServerHandshake serverHandshake)
	{

	}

	@Override
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

	@Override
	public void onClose(int i, String s, boolean b)
	{

	}

	@Override
	public void onError(Exception e)
	{

	}
}
