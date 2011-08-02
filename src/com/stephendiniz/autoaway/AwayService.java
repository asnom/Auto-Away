package com.stephendiniz.autoaway;

import java.util.ArrayList;

import android.app.PendingIntent;
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
	@Override
	public IBinder onBind(Intent i)
	{ return null; }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate();
        
        registerReceiver(smsreceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }
    
    private BroadcastReceiver smsreceiver = new BroadcastReceiver()
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

                Main mo = new Main();
                sendSms(info, mo.messageContent);
            }
        }
    };
    
    private void sendSms(String phonenumber, String message)
    {
        SmsManager manager = SmsManager.getDefault();
        
        PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);
        
        int length = message.length();
        
        if(length > 160)
        {
                ArrayList<String> messagelist = manager.divideMessage(message);
                
                manager.sendMultipartTextMessage(phonenumber, null, messagelist, null, null);
        }
        else
        {
                manager.sendTextMessage(phonenumber, null, message, piSend, piDelivered);
        }
    }
}
