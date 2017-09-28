package com.ksa.locationtube;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.ksa.locationtube.Common.isEmpty;

public class ContactsActivity extends AppCompatActivity {

	Logger logger = new Logger(getClass());
	ListView listView;
	private String selectedPhone;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts);
		listView = (ListView) findViewById(R.id.contacts_list);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.contacts_title);
		}
		listView.setAdapter(contactsAdapter);
		listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
				getMenuInflater().inflate(R.menu.contacts_context_menu, menu);
				try {
					selectedPhone = (String) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.getTag(); //((TextView)((RelativeLayout) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView).getChildAt(2)).getText().toString();
				} catch (Exception e) {
					logger.e("Error selecting contact " + e.getMessage(), e);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contacts_menu, menu);
		return true;
	}

	private BaseAdapter contactsAdapter = new BaseAdapter() {

		@Override
		public int getCount() {
			return Data.getContactsSize();
		}

		@Override
		public Object getItem(int position) {
			return Data.getContactsItem(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			try {
				View view = convertView;
				if (view == null) {
					LayoutInflater inflater = LayoutInflater.from(ContactsActivity.this);
					view = inflater.inflate(R.layout.contact_list_item_view, parent, false);
				}
				Contact contact = (Contact) getItem(position);
				if (contact != null) {
					TextView nameView = (TextView) view.findViewById(R.id.contact_item_name);
					nameView.setText(contact.getName());
					TextView phoneView = (TextView) view.findViewById(R.id.contact_item_phone);
					phoneView.setText(contact.getPhone());
					ImageView imageView = (ImageView) view.findViewById(R.id.contact_item_image);
					imageView.setImageBitmap(contact.getBitmap());
					view.setTag(contact.getPhone());
				}
				return view;
			} catch (Exception e) {
				logger.e("Error inflating list item view", e);
				throw e;
			}
		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add_contact_menu_item:
				openContacts();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.contacts_context_menu_delete_item:
				if (!isEmpty(selectedPhone)) {
					logger.i("Removing contact " + selectedPhone);
					Data.removeContact(selectedPhone);
					Data.save();
					contactsAdapter.notifyDataSetChanged();
				}
				return true;
		}
		return false;
	}

	private static final int RESULT_PICK_CONTACT = 10001;

	private void openContacts() {
		Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intent, RESULT_PICK_CONTACT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case RESULT_PICK_CONTACT:
					pickContact(data.getData());
					break;
			}
		}
	}

	private void pickContact(Uri uri) {
		if (uri != null) {
			try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
				if (cursor != null) {
					cursor.moveToFirst();
					String cid = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
					String phoneNumber = "";
					try (Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + cid, null, null))
					{
						if (phones != null) {
							while (phones.moveToNext()) {
								int index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
								if (index >= 0) {
									String phone = phones.getString(index);
									if (!isEmpty(phone)) {
										phoneNumber = phone;
										break;
									}
								}
							}
						}
						int index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);
						if (index >= 0) {
							logger.i("Picture URI is " + cursor.getString(index));
						}
					}
					String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
					if (isEmpty(phoneNumber)) {
						Toast.makeText(this, String.format(getString(R.string.add_contact_no_phone), name), Toast.LENGTH_LONG).show();
					} else {
						logger.i("Picked contact: " + phoneNumber + " " + name);
						Data.addContact(new Contact(phoneNumber, name, Integer.parseInt(cid)));
						Data.save();
						contactsAdapter.notifyDataSetChanged();
					}
				}
			} catch (Exception e) {
				logger.e("Error picking contact", e);
			}
		}
	}
}
