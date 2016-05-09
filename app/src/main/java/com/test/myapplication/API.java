package com.test.myapplication;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class API extends WebSocketClient
{
	public static final String ACTION_CONNECT_TO_WS = "com.test.myapplication.ACTION_CONNECT_TO_WS";
	public static final String ACTION_DB_NEW_ENTRY  = "com.test.myapplication.ACTION_DB_NEW_ENTRY";
	public static final String EXTRA_DB_NEW_ENTRY   = "com.test.myapplication.DB_NEW_ENTRY";

	public static final String TAG                      = "API";
	public static final int    STATE_CONNECTED          = 1;
	public static final int    STATE_DISCONNECTED       = 2;
	public static final int    STATE_DISCONNECTED_ERROR = 3;
	public static final String BASE_URL                 = "ws://mini-mdt.wheely.com";

	private static API mInstance = null;

	private static volatile String mPass;
	private static volatile String mLogin;

	private int mState = -1;
	private        APICallBack mAPICallBack;
	private static boolean     mIsAuthorized;

	public int getState()
	{
		return mState;
	}

	public void setState(int newState)
	{
		mState = newState;
	}

	public interface APICallBack
	{
		void onOpen(ServerHandshake serverHandshake);

		void onMessage(String s);

		void onClose(int i, String s, boolean b);

		void onError(Exception e);
	}

	private static URI getURIMy(String login, String pass)
	{
		if (TextUtils.isEmpty(login) || TextUtils.isEmpty(pass))
			return null;

		URI uri;
		try
		{
			uri = new URI(BASE_URL + "?username=" + URLEncoder.encode(login, "UTF-8") + "&password=" + URLEncoder.encode(pass, "UTF-8"));
		}
		catch (URISyntaxException | UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
		return uri;
	}

	public API(URI serverURI)
	{
		super(serverURI);
	}

	// Lazy Initialization (If required then only)
	public static API getInstance(String login, String pass)
	{
		if (mLogin == null && login != null)
			mLogin = login;
		if (mPass == null && pass != null)
			mPass = pass;

		if (mInstance == null)
		{
			// Thread Safe. Might be costly operation in some case
			synchronized (API.class)
			{
				if (mInstance == null)
					mInstance = new API(getURIMy(mLogin, mPass));
			}
		}
		return mInstance;
	}

	/**
	 * Saving auth state is not implemented yet
	 */
	public void setAuthorized(boolean authorized)
	{
		mIsAuthorized = authorized;
	}

	/**
	 * Check if user is authorized
	 */
	public static boolean isAuthorized()
	{
		return mIsAuthorized;
	}

	@Override
	public void onOpen(ServerHandshake serverHandshake)
	{
		setState(STATE_CONNECTED);

		if (mAPICallBack != null)
			mAPICallBack.onOpen(serverHandshake);
	}

	@Override
	public void onMessage(String s)
	{
		if (mAPICallBack != null)
			mAPICallBack.onMessage(s);
	}

	@Override
	public void onClose(int i, String s, boolean b)
	{
		setState(STATE_DISCONNECTED);

		if (mAPICallBack != null)
			mAPICallBack.onClose(i, s, b);
	}

	@Override
	public void onError(Exception e)
	{
		setState(STATE_DISCONNECTED_ERROR);

		if (mAPICallBack != null)
			mAPICallBack.onError(e);
	}

	public boolean connect(@Nullable final APICallBack apiCallBack)
	{
		if (apiCallBack != null)
			mAPICallBack = apiCallBack;

		this.connect();
		return true;
	}

}
