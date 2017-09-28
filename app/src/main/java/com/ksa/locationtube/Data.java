package com.ksa.locationtube;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by krivozyatev-sa on 22.09.2017
 */

public class Data {

	private static final String CONTACT_LIST_PREFERENCES_KEY = "com.ksa.locationtube.contacts";
	private Logger logger = new Logger(getClass());

	private static Data data = new Data();

	private Map<String, Contact> contactsMap = new LinkedHashMap<>();
	private List<Contact> contactsList = new LinkedList<>();

	private Data() {
		String contactsJson = getPreferences().getString(CONTACT_LIST_PREFERENCES_KEY, null);
		if (contactsJson != null) {
			try {
				Contact[] contacts = getGson().fromJson(contactsJson, Contact[].class);
				for (Contact contact : contacts) {
					appendContact(new Contact(contact.getPhone(), contact.getName(), contact.getContactId()));
				}
			} catch (Exception e) {
				logger.e("Error loading contacts", e);
			}
		}
	}

	public static void addContact(Contact contact) {
		data.appendContact(contact);
	}

	private void appendContact(Contact contact) {
		boolean exists = contactsMap.containsKey(contact.getPhone());
		contactsMap.put(contact.getPhone(), contact);
		if (!exists) {
			contactsList.add(contact);
		} else {
			for (int i = 0; i < contactsList.size(); i++) {
				if (contactsList.get(i).getPhone().equals(contact.getPhone())) {
					contactsList.set(i, contact);
					break;
				}
			}
		}
	}

	public static void removeContact(String phone) {
		data.deleteContact(phone);
	}

	private void deleteContact(String phone) {
		if (phone != null) {
			for (int i = 0; i < contactsList.size(); i++) {
				Contact contact = contactsList.get(i);
				if (phone.equals(contact.getPhone())) {
					contactsList.remove(i);
					break;
				}
			}
			contactsMap.remove(phone);
		}
	}

	public static Contact[] getContacts() {
		return data.contactsList.toArray(new Contact[data.contactsList.size()]);
	}

	public static Contact getContact(String phone) {
		return phone != null ? data.contactsMap.get(phone) : null;
	}

	public static int getContactsSize() {
		return data.contactsMap.size();
	}

	public static Contact getContactsItem(int position) {
		return data.contactsList.get(position);
	}

	public static void save() {
		Gson gson = getGson();
		String json = gson.toJson(data.contactsList);
		data.logger.i(json);
		getPreferences().edit().putString(CONTACT_LIST_PREFERENCES_KEY, json).apply();
	}

	private static Gson getGson() {
		return new GsonBuilder()
				.setExclusionStrategies(new ExclusionStrategy() {
					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						return f.getAnnotation(JsonSkipField.class) != null;
					}

					@Override
					public boolean shouldSkipClass(Class<?> clazz) {
						return false;
					}
				})
				.create();
	}

	private static SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(App.getContext());
	}
}
