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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Main extends PreferenceActivity implements OnPreferenceChangeListener
{
	final String SERVICE_PREF	= "serviceCheckBox";
	final String MESSAGE_PREF	= "messageEditText";
	final String DELAY_PREF		= "delayEditText";
	
	final int NOTIFICATION_ID	= 1;

	private boolean	serviceRunning;
	private String	messageContent;
	private int		delayDuration;
	
	Resources r;
	
	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	
	Preference serviceCheckBox;
	Preference messageEditText;
	Preference delayEditText;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		//Preference Objects
		serviceCheckBox = (Preference)findPreference(SERVICE_PREF);
		serviceCheckBox.setOnPreferenceChangeListener(this);
		
		messageEditText = (Preference)findPreference(MESSAGE_PREF);
		messageEditText.setOnPreferenceChangeListener(this);
		
		delayEditText = (Preference)findPreference(DELAY_PREF);
		delayEditText.setOnPreferenceChangeListener(this);
		
		r = getResources();
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		editor = prefs.edit();
	}
	
	public void onResume()
	{
		super.onResume();
		
		
		if(serviceRunning())
			setPreferenceStatus(false);
		
		setMessageContent(prefs.getString(MESSAGE_PREF, r.getString(R.string.message_content)));
		setDelayDuration(prefs.getString(DELAY_PREF, "30"));
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		
		//Save information as Strings
		savedInstanceState.putString("messageSaved", getMessageContent());
		savedInstanceState.putString("delaySaved", Integer.toString(getDelayDuration()));
	}
	
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
		super.onRestoreInstanceState(savedInstanceState);
		
		//Restore and set saved information from Bundle as correct types
		setMessageContent(savedInstanceState.getString("messageSaved"));
		setDelayDuration(savedInstanceState.getString("delaySaved"));
    }
	
	@Override
	public boolean onPreferenceChange(Preference p, Object o)
	{
		r = getResources();
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if(p.getKey().equals(SERVICE_PREF))
		{
			final Intent awayService = new Intent(this, AwayService.class);
				
			if(prefs.getBoolean(SERVICE_PREF, false))
			{
				setServiceStatus(false);
				destroyNotification();
				setPreferenceStatus(true);
				stopService(awayService);
			}
			
			else
			{
				setServiceStatus(true);
				createNotification();
				setPreferenceStatus(false);

				//Set Intent Extras
				awayService.putExtra("extraMessageContent", getMessageContent());
				awayService.putExtra("extraDelayDuration", Integer.toString(getDelayDuration()));

				//Start service and terminate activity
				startService(awayService);
				finish();
			}
		}
		else if(p.getKey().equals(MESSAGE_PREF))
			if(getMessageContent().equals(""))
				setMessageContent(r.getString(R.string.message_content));
		else if(p.getKey().equals(DELAY_PREF))
			if(getDelayDuration() < 0)
				setDelayDuration("30");
		
		return true;
	}
	
	private void createNotification()
	{
		r = getResources();
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		Notification notification = new Notification(R.drawable.notification_icon, r.getString(R.string.notification_ticker_text), System.currentTimeMillis());
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0);
		
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
	
	private void setPreferenceStatus(boolean status)
	{
		messageEditText.setEnabled(status);
		delayEditText.setEnabled(status);
	}
	
	//Getters and Setters for non-final variables
	//Sets private variables AND preference
	public boolean serviceRunning()							{ return serviceRunning;								}
	public void setServiceStatus(boolean serviceRunning)	{ this.serviceRunning = serviceRunning;	
															  prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	                                                          editor = prefs.edit();
															  editor.putBoolean(SERVICE_PREF, serviceRunning);	
															  editor.commit();										}

	public String getMessageContent() 						{ return messageContent;								}
	public void setMessageContent(String messageContent)	{ this.messageContent = messageContent;
															  editor.putString(MESSAGE_PREF, messageContent);
															  editor.commit();										}
	
	public int getDelayDuration()							{ return delayDuration;									}
	public void setDelayDuration(String delayDuration)		{ this.delayDuration = Integer.parseInt(delayDuration);	
														      editor = prefs.edit();
															  editor.putString(DELAY_PREF, delayDuration);
															  editor.commit();										}
}