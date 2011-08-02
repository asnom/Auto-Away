package com.stephendiniz.autoaway;

import java.util.ArrayList;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class AwayService extends Service
{
	final String MESSAGE_PREF = "messageEditText";
	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	
	private BroadcastReceiver smsReceiver;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate();
	}
	
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		
		smsReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Bundle bundle = intent.getExtras();        
				SmsMessage[] msgs = null;

				if(null != bundle)
				{
					String info = null;
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];

					for (int i = 0; i < msgs.length; i++)
					{
						msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
						info = msgs[i].getOriginatingAddress();
					}

					Resources r = getResources();
					
					//Preference Manager
					prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					editor = prefs.edit();
					
					sendSms(info, prefs.getString(MESSAGE_PREF, r.getString(R.string.message_content)));
				}
			}
		};
		
		registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
	}

	public void onDestroy()
	{
		super.onDestroy();
		
		//Make sure to destroy the Broadcast Receiver when the Auto-Away Service is destroyed
		unregisterReceiver(smsReceiver);
	}
	
	public void sendSms(String phonenumber, String message)
	{
		SmsManager manager = SmsManager.getDefault();

		int length = message.length();

		if (length > 160)
		{
			ArrayList<String> messagelist = manager.divideMessage(message);

			manager.sendMultipartTextMessage(phonenumber, null, messagelist, null, null);
		}
		else
		{
			manager.sendTextMessage(phonenumber, null, message, null, null);
		}
	}
	
	@Override
	public IBinder onBind(Intent i) { return null; }
}