/*  Copyright 2013 Adam Markham
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package com.apexlabs.alarm;

import java.util.Random;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class AlarmService extends Service {
	private static final String TAG = "MyService";
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotificationManager;
	int mId;
	boolean notificationPosted = false;

	SharedPreferences prefs;
	String screenOnKey = "screenon";
	
	IntentFilter filter;
	BroadcastReceiver mReceiver;
	
	SharedPreferences.OnSharedPreferenceChangeListener listener;
	SharedPreferences.Editor editor;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	Handler handler = new Handler();
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		editor = prefs.edit();
		
		filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
		
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        		Log.d(TAG,"Shared Preferences Change Listener");
        		if (key.equals(screenOnKey)) {
        			boolean b = prefs.getBoolean(screenOnKey, false); 
        			if (b) {
        				Log.d(TAG,"Screen is On!");
        				handler.removeCallbacks(r);
        				handler.postDelayed(r, 1);
        			} else {
        				Log.d(TAG,"Screen is Off!");
        				clearNotification(867);
        				handler.removeCallbacks(r);
        			}
        		}
        	}
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		
		unregisterReceiver(mReceiver);
		clearNotification(867);
		handler.removeCallbacks(r);
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Log.d(TAG, "onStart");
		handler.postDelayed(r, 1000);
	}
	
	private Intent getClockIntent() {
		PackageManager packageManager = getPackageManager();
		Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
	 
		// Verify clock implementation
		String clockImpls[][] = {
				{ "HTC", "com.htc.android.worldclock",
				"com.htc.android.worldclock.WorldClockTabControl" },
				{ "Standard", "com.android.deskclock", 
				"com.android.deskclock.AlarmClock" },
				{ "Froyo", "com.google.android.deskclock", 
				"com.android.deskclock.DeskClock" },
				{ "Motorola", "com.motorola.blur.alarmclock",
				"com.motorola.blur.alarmclock.AlarmClock" },
				{ "Sony Ericsson", "com.sonyericsson.alarm", "com.sonyericsson.alarm.Alarm" },
				{ "Samsung", "com.sec.android.app.clockpackage", 
				"com.sec.android.app.clockpackage.ClockPackage" } };
	 
		boolean foundClockImpl = false;
	 
		for (int i = 0; i < clockImpls.length; i++) {
			String packageName = clockImpls[i][1];
			String className = clockImpls[i][2];
			try {
				ComponentName cn = new ComponentName(packageName, className);
				packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
				alarmClockIntent.setComponent(cn);
				foundClockImpl = true;
				break;
			} catch (NameNotFoundException e) {
				Log.d(TAG, "Alarm clock "+clockImpls[i][0]+" not found");
			}
		}
		
		if (foundClockImpl) {
			alarmClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		return alarmClockIntent;
	}

	boolean isAlarmActive() {
		Log.d(TAG, "isAlarmActive");
		if (Settings.System.getString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED).toString().length() < 2) {
			Log.d(TAG,"No Alarm Set");
			return false;
		} else {
			Log.d(TAG,"Alarm Set");
			return true;
		}
	}
	
	private void postNotification() {
		Log.d(TAG, "posting notification");
		mBuilder =
		        new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.transparent)
				//.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.alarm_white))
		        .setContentTitle(getResources().getString(R.string.notification_title))
		        .setContentText("Next Alarm is " + Settings.System.getString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED).toString());
		
		notificationPosted = true;
		Intent nIntent = getClockIntent();
		nIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(AlarmNotification.class);
		stackBuilder.addNextIntent(nIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		
		// should make mId random
		mId = 867;
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(mId, mBuilder.build());
	}
	
	private void clearNotification(int id) {
		Log.d(TAG,"Clearing Notification");
		notificationPosted = false;
		
		try{
			mNotificationManager.cancel(id);
		} catch (NullPointerException e) {
			Log.d(TAG,e + "");
		}
	}
	
	final Runnable r = new Runnable() {
	    public void run() {
	    	Log.d(TAG,"Runnable");
	    	if(isAlarmActive()) {
	    		Log.d(TAG,"Alarm Active");
	    		if(!notificationPosted) {
	    			Log.d(TAG,"Runnable Posting Notification");
	    			postNotification();
	    		}
	    	} else {
	    		clearNotification(867);
	    	}
	        handler.postDelayed(this, 5000);
	    }
	};
}
