package com.ksa.locationtube;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import com.google.gson.annotations.SerializedName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by krivozyatev-sa on 22.09.2017
 */

public class Contact {

	@SerializedName("p")
	private String phone;
	@SerializedName("n")
	private String name;
	@SerializedName("id")
	private int contactId;

	@JsonSkipField
	private Bitmap bitmap = EMPTY_BITMAP;
	@JsonSkipField
	private Logger logger = new Logger(getClass());

	private static final Bitmap EMPTY_BITMAP = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);

	public Contact(String phone, String name, int contactId) {
		this.phone = phone;
		this.name = name;
		this.contactId = contactId;
		fillBitmap();
	}

	public String getPhone() {
		return phone;
	}

	public String getName() {
		return name;
	}

	public int getContactId() {
		return contactId;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public boolean hasBitmap() {
		return bitmap != EMPTY_BITMAP;
	}

	private void fillBitmap() {
		Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
		try (InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(App.getContext().getContentResolver(),contactUri)) {
			if (inputStream != null) {
				BufferedInputStream buf = new BufferedInputStream(inputStream);
				bitmap = BitmapFactory.decodeStream(buf);
			}
		} catch (IOException e) {
			logger.e("Error getting bitmap for contact " + contactId, e);
		}
	}
}
