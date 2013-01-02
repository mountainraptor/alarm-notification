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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

	private static final String TAG = "Screen Receiver";
    String screenOnKey = "screenon";
    SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
    	
    	if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
        	Log.d(TAG, "Screen Off");
        	editor.putBoolean(screenOnKey, false);
        	editor.commit();
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
        	Log.d(TAG, "Screen On");
        	editor.putBoolean(screenOnKey, true);
        	editor.commit();
        }
    }
}


