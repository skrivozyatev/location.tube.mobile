package com.ksa.locationtube;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/**
 * Created by Sergey Krivozyatev on 28.07.2017 13:16
 */

public class Settings implements Serializable {

	private Logger logger = new Logger(getClass());

	private final static Settings settings = new Settings();
	private File settingsFile = new File("settings.properties");
	private Properties properties = new Properties();
	private Context context = App.getContext();

	public static Settings get() {
		return settings;
	}

	private Settings() {
		load();
	}

	private String url = Constants.URL;
	private String myPhoneNumber = null;
	private int myLocationScanInterval = 60;
	private int locationsRefreshInterval = 5;
	private int myLocationAccuracy = 50;

	public String getUrl() {
		return url;
	}

	private void load() {
		if (settingsFile.exists()) {
			try (FileInputStream fis = context.openFileInput(settingsFile.getName())) {
				properties.load(fis);
				for (Setting setting : Setting.values()) {
					loadSetting(setting);
				}
			} catch (IOException e) {
				logger.e("Error reading settings from the file " + settingsFile.getName(), e);
			}
		}
	}

	private void loadSetting(Setting setting) {
		String value = properties.getProperty(setting.getName());
		if (value != null) {
			switch (setting) {
				case Url: url = value; break;
				case Phone: myPhoneNumber = value; break;
				case MyLocationScanInterval: myLocationScanInterval = parseInt(value); break;
				case LocationsRefreshInterval: locationsRefreshInterval = parseInt(value); break;
				case MyLocationAccuracy: myLocationAccuracy = parseInt(value); break;
			}
		}
	}

	private int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public void save() {
		try (FileOutputStream fos = context.openFileOutput(settingsFile.getName(), Context.MODE_PRIVATE)) {
			saveSetting(Setting.Url, url);
			saveSetting(Setting.Phone, myPhoneNumber);
			saveIntSetting(Setting.MyLocationScanInterval, myLocationScanInterval);
			saveIntSetting(Setting.LocationsRefreshInterval, locationsRefreshInterval);
			saveIntSetting(Setting.MyLocationAccuracy, myLocationAccuracy);
			properties.store(fos, "Location Tube Settings");
		} catch (IOException e) {
			logger.e("Error save setting to the file " + settingsFile.getName());
		}
	}

	private void saveSetting(Setting setting, String value) {
		properties.setProperty(setting.getName(), value);
	}

	private void saveIntSetting(Setting setting, int value) {
		saveSetting(setting, String.valueOf(value));
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMyPhoneNumber() {
		return myPhoneNumber;
	}

	public void setMyPhoneNumber(String myPhoneNumber) {
		this.myPhoneNumber = myPhoneNumber;
	}

	public int getMyLocationScanInterval() {
		return myLocationScanInterval;
	}

	public void setMyLocationScanInterval(int myLocationScanInterval) {
		this.myLocationScanInterval = myLocationScanInterval;
	}

	public int getLocationsRefreshInterval() {
		return locationsRefreshInterval;
	}

	public void setLocationsRefreshInterval(int locationsRefreshInterval) {
		this.locationsRefreshInterval = locationsRefreshInterval;
	}

	public int getMyLocationAccuracy() {
		return myLocationAccuracy;
	}

	public void setMyLocationAccuracy(int myLocationAccuracy) {
		this.myLocationAccuracy = myLocationAccuracy;
	}

	private enum Setting {
		Url("url"), Phone("phone"), MyLocationScanInterval("my.location.scan.interval"),
		LocationsRefreshInterval("locations.refresh.interval"), MyLocationAccuracy("my.location.accuracy");

		private final String name;

		Setting(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
