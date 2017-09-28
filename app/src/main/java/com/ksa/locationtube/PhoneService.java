package com.ksa.locationtube;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.IOException;

import ksa.location.tube.client.RetrofitClient;
import ksa.location.tube.model.Location;
import ksa.location.tube.model.UserLocation;

import static com.ksa.locationtube.Common.isEmpty;

/**
 * Created by Sergey Krivozyatev on 25.07.2017 9:44
 */

public class PhoneService {

	private static PhoneService phoneService;

	private Context applicationContext;
	private SharedPreferences preferences;

	private String phonePreferencesKey;

	public static PhoneService create(Context applicationContext) {
		if (phoneService == null) {
			phoneService = new PhoneService(applicationContext);
		}
		return phoneService;
	}

	public static PhoneService get() {
		if (phoneService != null) {
			return phoneService;
		}
		throw new RuntimeException("PhoneService is not initialized");
	}

	private PhoneService(Context applicationContext) {
		this.applicationContext = applicationContext;
		Resources resources = applicationContext.getResources();
		preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		phonePreferencesKey = resources.getString(R.string.phone_pref_key);
		getPhoneNumber();
	}

	public String getPhoneNumber() {
		String phoneNumber = preferences.getString(phonePreferencesKey, null);
		if (isEmpty(phoneNumber)) {
			TelephonyManager telephonyManager = (TelephonyManager) applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
			phoneNumber = telephonyManager.getLine1Number();
			preferences.edit().putString(phonePreferencesKey, phoneNumber).apply();
		}
		return phoneNumber;
	}
}
