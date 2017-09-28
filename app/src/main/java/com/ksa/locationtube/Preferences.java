package com.ksa.locationtube;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ksa.locationtube.Common.isEmpty;

/**
 * Created by Sergey Krivozyatev on 01.08.2017 14:27
 */

public class Preferences {

	private static Preferences preferences;

	private Context applicationContext;
	private SharedPreferences sharedPreferences;

	private String urlPreferencesKey;
	private String phonePreferencesKey;
	private String myLocationScanIntervalPreferencesKey;
	private String locationsRefreshIntervalPreferencesKey;
	private String myLocationAccuracyPreferencesKey;
	private String locationSetupDurationPreferencesKey;
	private String publishSwitchPreferencesKey;

	public static Preferences get() {
		if (preferences == null) {
			preferences = new Preferences();
		}
		return preferences;
	}

	private Preferences() {
		applicationContext = App.getContext();
		Resources resources = applicationContext.getResources();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

		urlPreferencesKey = resources.getString(R.string.url_pref_key);
		phonePreferencesKey = resources.getString(R.string.phone_pref_key);
		myLocationScanIntervalPreferencesKey = resources.getString(R.string.my_location_scan_interval_pref_key);
		locationsRefreshIntervalPreferencesKey = resources.getString(R.string.locations_refresh_interval_pref_key);
		myLocationAccuracyPreferencesKey = resources.getString(R.string.location_accuracy_pref_key);
		locationSetupDurationPreferencesKey = resources.getString(R.string.location_setup_duration_pref_key);
		publishSwitchPreferencesKey = resources.getString(R.string.publish_switch_pref_key);

		getPhoneNumber();
	}

	public String getUrl() {
		return sharedPreferences.getString(urlPreferencesKey, Constants.URL);
	}

	public String getPhoneNumber() {
		String phoneNumber = sharedPreferences.getString(phonePreferencesKey, null);
		if (isEmpty(phoneNumber)) {
			TelephonyManager telephonyManager = (TelephonyManager) applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
			if (telephonyManager != null) {
				try {
					phoneNumber = telephonyManager.getLine1Number();
				} catch (SecurityException e) {

				}
			}
			if (!isEmpty(phoneNumber)) {
				sharedPreferences.edit().putString(phonePreferencesKey, phoneNumber).apply();
			}
		}
		return phoneNumber;
	}

	public boolean getPublishSwitch() {
		return sharedPreferences.getBoolean(publishSwitchPreferencesKey, true);
	}

	public int getMyLocationScanInterval() {
		return Integer.parseInt(sharedPreferences.getString(myLocationScanIntervalPreferencesKey, "60"));
	}

	public int getLocationsRefreshInterval() {
		return Integer.parseInt(sharedPreferences.getString(locationsRefreshIntervalPreferencesKey, "5"));
	}

	public int getMyLocationAccuracy() {
		return Integer.parseInt(sharedPreferences.getString(myLocationAccuracyPreferencesKey, "50"));
	}

	public int getLocationSetupDuration() {
		return Integer.parseInt(sharedPreferences.getString(locationSetupDurationPreferencesKey, "1"));
	}

	public SharedPreferences getSharedPreferences() {
		return sharedPreferences;
	}

	public String getPublishSwitchPreferencesKey() {
		return publishSwitchPreferencesKey;
	}
}
