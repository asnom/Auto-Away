package com.stephendiniz.autoaway;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class AwayService extends Service
{
	final String MESSAGE_PREF	= "messageEditText";
	final String DELAY_PREF		= "delayEditText";

	private boolean informStatus;
	private String messageContent;
	private int delayDuration;
	private boolean repeatStatus;
	private String returnAddress;
	
	List<String> addresses;
	
	Timer timer = new Timer();
	Bundle infoBundle;

	BroadcastReceiver smsReceiver;
	
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		infoBundle = intent.getExtras();
		setMessageContent(infoBundle.getString("extraMessageContent"));
		setInformStatus(infoBundle.getBoolean("extraInformStatus"));
		setDelayDuration(infoBundle.getString("extraDelayDuration"));
		setRepeatStatus(infoBundle.getBoolean("extraRepeatStatus"));
		
		smsReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Bundle bundle = intent.getExtras();
				SmsMessage[] msgs = null;

				if(null != bundle)
				{
					setReturnAddress(null);
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];

					for (int i = 0; i < msgs.length; i++)
					{
						msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
						setReturnAddress(msgs[i].getOriginatingAddress());
					}
					
					repeatCheck();
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

	public void setDelay()
	{
		timer.schedule(new TimerTask()
		{
			public void run()
			{
				sendSms();
			}
		},(long)(1000*getDelayDuration()));
	}
	
	public void repeatCheck()
	{
		if(!getRepeatStatus() || !(addresses.contains(getReturnAddress())))
			setDelay();
		
		addresses.add(getReturnAddress());
	}
	
	public void sendSms()
	{
		SmsManager manager = SmsManager.getDefault();
		
		int length = getMessageContent().length();

		if (length > 160)
		{
			ArrayList<String> messagelist = manager.divideMessage(getMessageContent());

			manager.sendMultipartTextMessage(getReturnAddress(), null, messagelist, null, null);
		}
		else
			manager.sendTextMessage(getReturnAddress(), null, getMessageContent(), null, null);
	}

	@Override
	public IBinder onBind(Intent i) { return null; }
	
	//Getters and Setters for non-final variables
	//Sets private variables AND preference
	public String getMessageContent() 						{ if(getInformStatus()) { return "[Auto-Away]: " + messageContent;	}
															  return messageContent;											}
	public void setMessageContent(String messageContent)	{ this.messageContent = messageContent;								}

	public boolean getInformStatus()						{ return informStatus;												}
	public void setInformStatus(boolean informStatus)		{ this.informStatus = informStatus;									}
	
	public int getDelayDuration()							{ return delayDuration;												}
	public void setDelayDuration(String delayDuration)		{ this.delayDuration = Integer.parseInt(delayDuration);				}
	
	public String getReturnAddress()						{ return returnAddress;												}
	public void setReturnAddress(String returnAddress)		{ this.returnAddress = returnAddress;								}
	
	public boolean getRepeatStatus()						{ return repeatStatus;												}
	public void setRepeatStatus(boolean repeatStatus)		{ this.repeatStatus = repeatStatus;									}
}