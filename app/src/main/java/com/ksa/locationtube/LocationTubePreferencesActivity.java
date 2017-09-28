package com.ksa.locationtube;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

/**
 * Created by Sergey Krivozyatev on 31.07.2017 18:17
 */

public class LocationTubePreferencesActivity extends PreferenceActivity {

	private final Logger logger = new Logger(getClass());

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new LocationTubePreferencesFragment()).commit();
		Preferences.get().getSharedPreferences().registerOnSharedPreferenceChangeListener(changeListener);
	}

	public static class LocationTubePreferencesFragment extends PreferenceFragment {

		@Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
		}
	}

	private SharedPreferences.OnSharedPreferenceChangeListener changeListener =
			new SharedPreferences.OnSharedPreferenceChangeListener() {
				@Override
				public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
					if (Preferences.get().getPublishSwitchPreferencesKey().equals(key)) {
						if (Preferences.get().getPublishSwitch()) {
							logger.i("Start publish service from preferences");
							startService(new Intent(getApplicationContext(), LocationPublishService.class));
						} else {
							logger.i("Stop publish service from preferences");
							stopService(new Intent(getApplicationContext(), LocationPublishService.class));
						}
					}
				}
			};
}
