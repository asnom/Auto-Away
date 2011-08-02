package com.stephendiniz.autoaway;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Main extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener
{
	final String SERVICE_PREF = "serviceCheckBox";
	final String MESSAGE_PREF = "messageEditText";
	final int NOTIFICATION_ID = 1;
	
	String messageContent;

	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		//Preference Objects
		Preference messageEditText = (Preference)findPreference(MESSAGE_PREF);
		messageEditText.setOnPreferenceClickListener(this);
		
		Preference serviceCheckBox = (Preference)findPreference(SERVICE_PREF);
		serviceCheckBox.setOnPreferenceChangeListener(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		savedInstanceState.putString("messageSaved", messageContent);
		super.onSaveInstanceState(savedInstanceState);
	}
	
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
      super.onRestoreInstanceState(savedInstanceState);
      messageContent = savedInstanceState.getString("messageSaved");
      
      prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
      editor = prefs.edit();
      editor.putString(MESSAGE_PREF, messageContent);
      editor.commit();
    }
	
	@Override
	public boolean onPreferenceChange(Preference p, Object o)
	{
		Resources r = getResources();
		
		//Preference Manager
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		editor = prefs.edit();
		
		//Check and Edit Preferences
		if(p.getKey().equals(SERVICE_PREF))
		{
			final Intent awayService = new Intent(this, AwayService.class);

			if(prefs.getBoolean(SERVICE_PREF, false))
			{
				editor.putBoolean(SERVICE_PREF, false);
				destroyNotification();
				
				stopService(awayService);
			}
			
			else
			{
				editor.putBoolean(SERVICE_PREF, true);
				createNotification();

				startService(awayService);
				
				finish();
			}
		}
		else if(p.getKey().equals(MESSAGE_PREF))
			messageContent = prefs.getString(MESSAGE_PREF, r.getString(R.string.message_content));
		
		//Commit and return true
		editor.commit();
		
		return true;
	}
	
	@Override
	public boolean onPreferenceClick(Preference p)
	{
		Resources r = getResources();
		
		//Preference Manager
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		editor = prefs.edit();
		
		//Check and Edit Preferences
		if(p.getKey().equals(MESSAGE_PREF))
			if(prefs.getString(MESSAGE_PREF, null).equals(""))
				editor.putString(MESSAGE_PREF, r.getString(R.string.message_content));
		
		//Commit and return true
		editor.commit();
		
		return true;
	}
	
	private void createNotification()
	{
		Resources r = getResources();
		
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		long timeToMake = System.currentTimeMillis();
		
		Notification notification = new Notification(R.drawable.notification_icon, r.getString(R.string.notification_ticker_text), timeToMake);
		
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this, r.getString(R.string.notification_title), r.getString(R.string.notification_content), contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		
		nManager.notify(NOTIFICATION_ID, notification);
	}
	
	private void destroyNotification()
	{
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nManager.cancel(NOTIFICATION_ID);
	}
}