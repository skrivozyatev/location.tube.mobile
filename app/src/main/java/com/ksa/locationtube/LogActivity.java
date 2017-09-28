package com.ksa.locationtube;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LogActivity extends AppCompatActivity {

	private Logger logger = new Logger(getClass());

	private Handler handler;

	private EditText logEditText;
	private boolean active = false;
	private boolean paused = false;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		//Toolbar toolbar = (Toolbar) findViewById(R.id.log_toolbar);
		//setSupportActionBar(toolbar);
		logEditText = (EditText) findViewById(R.id.logEditText);
		handler = new Handler();
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.log_title);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.log_menu, menu);
		MenuItem switchItem = menu.findItem(R.id.log_switch);
		Switch sw = getLogSwitch(switchItem);
		getLogSwitch(switchItem).setChecked(true);
		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					logger.i("Resuming log");
					resumeLog();
				} else {
					pauseLog();
					logger.i("Log is paused");
				}
			}
		});
		return true;
	}

	private static String lastTime = null;

	private Thread thread = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				logger.i("Log read process started");
				String command = "logcat -v time";
				command += " LocationTube:D";
				logger.i("Executing the command: " + command);
				Process process = Runtime.getRuntime().exec(command);
				final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = bufferedReader.readLine();
				while (active && line != null) {
					if (!paused) {
						if (line.contains("[LocationTube]")) {
							final String text = line + "\n\n";
							handler.post(new Runnable() {
								@Override
								public void run() {
									logEditText.append(text);
								}
							});
							lastTime = line.substring(0, line.indexOf(' ', line.indexOf(' ') + 1));
						}
						line = bufferedReader.readLine();
					} else {
						Thread.sleep(500);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.e("Error reading log", e);
			} finally {
				logger.i("Log read process stopped at " + lastTime);
			}
		}
	});

	private Switch getLogSwitch(MenuItem item) {
		return (Switch) ((RelativeLayout) item.getActionView()).getChildAt(0);
	}

	@Override
	protected void onStart() {
		super.onStart();
		startLog();
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopLog();
	}

	private void startLog() {
		if (!active) {
			logEditText.setText("");
			active = true;
			thread.start();
		}
	}

	private void stopLog() {
		if (active) {
			active = false;
			thread.interrupt();
		}
	}

	private void pauseLog() {
		paused = true;
	}

	private void resumeLog() {
		paused = false;
	}
}
