package com.ksa.locationtube;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		Settings settings = Settings.get();

		setEditTextValue(R.id.editTextURL, settings.getUrl());
		setEditTextValue(R.id.editTextPhone, settings.getMyPhoneNumber());
		setEditTextValue(R.id.editTextMyLocatinRefreshInterval, String.valueOf(settings.getMyLocationScanInterval()));
		setEditTextValue(R.id.editTextLocationsRefreshInterval, String.valueOf(settings.getLocationsRefreshInterval()));
		setEditTextValue(R.id.editTextMyLocationAccuracy, String.valueOf(settings.getMyLocationAccuracy()));
	}

	private void setEditTextValue(int id, String value) {
		EditText editText = (EditText) findViewById(id);
		editText.setText(value);
	}

	public void saveSettings(View view) {
		Settings settings = Settings.get();
		settings.setUrl(getEditTextValue(R.id.editTextURL));
		settings.setMyPhoneNumber(getEditTextValue(R.id.editTextPhone));
		settings.setMyLocationScanInterval(getIntEditTextValue(R.id.editTextMyLocatinRefreshInterval));
		settings.setLocationsRefreshInterval(getIntEditTextValue(R.id.editTextLocationsRefreshInterval));
		settings.setMyLocationAccuracy(getIntEditTextValue(R.id.editTextMyLocationAccuracy));
		settings.save();
		finish();
	}

	private String getEditTextValue(int id) {
		EditText editText = (EditText) findViewById(id);
		return editText.getText().toString();
	}

	private int getIntEditTextValue(int id) {
		String str = getEditTextValue(id);
		return Integer.parseInt(str);
	}
}
