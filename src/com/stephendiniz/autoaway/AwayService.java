package com.stephendiniz.autoaway;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class AwayService extends Service
{
	private boolean informStatus;
	private String messageContent;
	private int delayDuration;
	private boolean logStatus;
	private boolean repeatStatus;
	private String returnAddress;
	private int notifyCount;
	
	final int NOTIFICATION_ID = 2;
	
	private List<String> addresses = new ArrayList<String>();
	
	private Timer timer = new Timer();
	private Bundle infoBundle;
	
	Resources r;
	
	BroadcastReceiver smsReceiver;
	
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		
		setNotifyCount(1);
		
		infoBundle = intent.getExtras();
		setMessageContent(infoBundle.getString("extraMessageContent"));
		setInformStatus(infoBundle.getBoolean("extraInformStatus"));
		setDelayDuration(infoBundle.getString("extraDelayDuration"));
		setLogStatus(infoBundle.getBoolean("extraLogStatus"));
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
		addresses.clear();
		timer.cancel();
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
		if(getRepeatStatus() || !(addresses.contains(getReturnAddress())))
			setDelay();

		if(!addresses.contains(getReturnAddress()))
			addresses.add(getReturnAddress());
	}
	
	private void notifySent()
	{
		r = getResources();
			NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification;

				if(getNotifyCount() > 1)
				{
					//Destroy old Notification
					nManager.cancel(NOTIFICATION_ID);

					notification = new Notification(R.drawable.notification_icon, r.getString(R.string.nlog_ticker_text) + " " + getReturnAddress(), System.currentTimeMillis());
					notification.setLatestEventInfo(this, r.getString(R.string.nlog_title), r.getString(R.string.nlog_content) + " " + getNotifyCount() + " " + r.getString(R.string.nlog_content_2), PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0));
				}
				else
				{
					notification = new Notification(R.drawable.notification_icon, r.getString(R.string.nlog_ticker_text) + " " + getReturnAddress(), System.currentTimeMillis());
					notification.setLatestEventInfo(this, r.getString(R.string.nlog_title), r.getString(R.string.nlog_content) + " " + getReturnAddress(), PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0));
				}

				nManager.notify(NOTIFICATION_ID, notification);
				setNotifyCount(getNotifyCount() + 1);
	}
	
	public void sendSms()
	{
		SmsManager manager = SmsManager.getDefault();
		int length = getMessageContent().length();
		
		if(getLogStatus())
			notifySent();

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
	
	public boolean getLogStatus()              				{ return logStatus;                         						}
	public void setLogStatus(boolean logStatus)        		{ this.logStatus = logStatus;                    					}
	
	public boolean getRepeatStatus()						{ return repeatStatus;												}
	public void setRepeatStatus(boolean repeatStatus)		{ this.repeatStatus = repeatStatus;									}
	
	public int getNotifyCount()                				{ return notifyCount;                        						}
	public void setNotifyCount(int notifyCount)        		{ this.notifyCount = notifyCount;                  					}
}