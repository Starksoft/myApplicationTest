package com.test.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver
{
	private static final String TAG = "MyReceiver";

	public MyReceiver()
	{
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent == null)
			return;

		switch (intent.getAction())
		{
			case Intent.ACTION_BOOT_COMPLETED:
				context.startService(new Intent(context, DataCollectingService.class));
				break;
		}
	}
}
