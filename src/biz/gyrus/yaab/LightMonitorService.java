/*
 * YAAB concept proof project, (C) Gyrus Solutions, 2011
 * http://www.gyrus.biz
 * 
 */

package biz.gyrus.yaab;

import java.util.LinkedList;
import java.util.List;

import biz.gyrus.yaab.BrightnessController.ServiceStatus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;

public class LightMonitorService extends Service {

	public static final float HIST_DELTA_THRESHOLD = 0.02f;
	
	private static class Reading
	{
		public Reading(float f, long t) { fVal = f; lTime = t; }
		
		public float fVal = 0f;
		public long lTime = 0L;
	}

	private boolean _bActive = false;
	private Handler _h = new Handler();
	private SensorManager _sensorManager = null;
	private Sensor _lightSensor = null;

	private float _currentRunningReading = 150f;
	private List<Reading> _readings = new LinkedList<Reading>();

	private ActivatorView _av = null;
	private WindowManager.LayoutParams _avLayoutParams = null;

	private static LightMonitorService _instance = null;
	
	private PendingIntent _piSelf = null;

	public static LightMonitorService getInstance() {
		return _instance;
	}

	private BroadcastReceiver _brScrOFF = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				Log.i(Globals.TAG,
						"ScreenOFF broadcast received, unregistering listeners.");

				if (_sensorManager != null)
					_sensorManager.unregisterListener(_listener);

				cancelTimer();

				AppSettings as = new AppSettings(LightMonitorService.this);
				if(!as.getPersistAlwaysNotification())
					showNotificationIcon(false);
				startAlertKeepalive(false);
			}
		}
	};

	private BroadcastReceiver _brScrON = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				Log.i(Globals.TAG,
						"ScreenON broadcast received, registering listeners.");

				_sensorManager.registerListener(_listener, _lightSensor,
						SensorManager.SENSOR_DELAY_FASTEST);

				AppSettings as = new AppSettings(LightMonitorService.this);
				showNotificationIcon(as.getPersistNotification());
				startAlertKeepalive(as.getAlertKeepalive());
			}

		}
	};

	private Runnable _timerHandler = new Runnable() {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			Log.d(Globals.TAG, String.format("Timer hit, millis: %d.", now));

			// cleaning obsolete values which are out of measuring frame
			while(_readings.size() > 0 && _readings.get(0).lTime < now - Globals.MEASURING_FRAME)
				_readings.remove(0);
			
			if (_readings.size() > 0) {
				
				float sum = 0;
				long measurePoint = now;
				
				for (int i = _readings.size() - 1; i >= 0; i--) 
				{
					Reading r = _readings.get(i);
					
					sum += r.fVal * (measurePoint - r.lTime);
					measurePoint = r.lTime;
				}
				
				sum += _currentRunningReading * (measurePoint - (now - Globals.MEASURING_FRAME));
				
				float currentReading = sum / Globals.MEASURING_FRAME;

				Log.d(Globals.TAG, "AVG reading: " + currentReading);

				float currentReadingBrightness = BrightnessController.get().getBrightnessFromReading(currentReading);
				float currentRunningBrightness = BrightnessController.get().getBrightnessFromReading(_currentRunningReading);

				Log.d(Globals.TAG, "ReadingBrightness: " + currentReadingBrightness);
				Log.d(Globals.TAG, "RunningBrightness: " + currentRunningBrightness);

				if (Math.abs(currentReadingBrightness
						- currentRunningBrightness) > HIST_DELTA_THRESHOLD) {
					Log.d(Globals.TAG, "Threshold defeated!");
					_currentRunningReading = currentReading;
					applyRunningReading();
				}
			} else {
				_bActive = false;
			}

			if(_bActive)
				_h.postDelayed(_timerHandler, Globals.TIMER_PERIOD);
		}
	};

	private SensorEventListener _listener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Log.d(Globals.TAG, "Accuracy changed called!");
		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
				float currentReading = event.values[0];
				Log.d(Globals.TAG,
						String.format("Float brightness: %f", currentReading));
				
				_readings.add(new Reading(currentReading, System.currentTimeMillis()));

				kickTimer();
			}

		}
	};

	public synchronized void applyRunningReading() {
		setBrightness(BrightnessController.get().getBrightnessFromReading(
				_currentRunningReading));
	}

	@Override
	public void onCreate() {
		_instance = this;
		Log.i(Globals.TAG, "Service onCreate() called");

		super.onCreate();

		if (BrightnessController.get().isLightSensorPresent(this)) {
			AppSettings as = new AppSettings(this);

			_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			_lightSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

			_piSelf = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), PendingIntent.FLAG_UPDATE_CURRENT);

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (pm.isScreenOn()) {
				_sensorManager.registerListener(_listener, _lightSensor, SensorManager.SENSOR_DELAY_FASTEST);

				showNotificationIcon(as.getPersistNotification());

				startAlertKeepalive(as.getAlertKeepalive());
				Log.i(Globals.TAG, "Listener registered");
			} else
				Log.i(Globals.TAG, "Screen is off, skip listener registration.");

			BrightnessController.get().setManualAdjustment(as.getAdjshift());

			_av = new ActivatorView(this);
			_avLayoutParams = new WindowManager.LayoutParams(0, 0, 0, 0,
					WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
							| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
					PixelFormat.OPAQUE);
			_avLayoutParams.screenBrightness = 20f;

			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.addView(_av, _avLayoutParams);

			registerReceiver(_brScrOFF, new IntentFilter(Intent.ACTION_SCREEN_OFF));
			registerReceiver(_brScrON, new IntentFilter(Intent.ACTION_SCREEN_ON));

			BrightnessController.get().updateServiceStatus(ServiceStatus.Running);
			
		}

		Log.i(Globals.TAG, "Service onCreate() finished");
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(Globals.TAG, "Service onStartCommand() called");

		if (BrightnessController.get().isLightSensorPresent(this)) {
			int brightnessMode = 0;

			try {
				brightnessMode = Settings.System.getInt(getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS_MODE);
			} catch (SettingNotFoundException e) {
				e.printStackTrace();
			}

			if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				Settings.System.putInt(getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS_MODE,
						Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
		} else {
			Log.w(Globals.TAG,
					"Started on a device without light sensor. How could this happen at all?!!");
			stopSelf();
		}

		return super.onStartCommand(intent, flags, startId);
	};

	@Override
	public void onDestroy() {

		Log.i(Globals.TAG, "Service onDestroy() called");
		BrightnessController.get().updateServiceStatus(ServiceStatus.Stopped);

		unregisterReceiver(_brScrOFF);
		unregisterReceiver(_brScrON);

		if (_sensorManager != null && _listener != null)
			_sensorManager.unregisterListener(_listener);

		cancelTimer();

		if (_av != null) {
			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.removeView(_av);
			_av = null;
		}

		showNotificationIcon(false);

		super.onDestroy();
		_instance = null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	// must be called when incoming sensor change arrives
	private void kickTimer() {
		Log.i(Globals.TAG, "kickTimer in action");
		if (_bActive)
			return;

		_bActive = true;

		_h.postDelayed(_timerHandler, Globals.TIMER_PERIOD);
		Log.i(Globals.TAG, "Timer hit initiated!");
	}

	private void cancelTimer() {
		Log.i(Globals.TAG, "cancelTimer called");
		_h.removeCallbacks(_timerHandler);
		_bActive = false;
	}

	private void setBrightness(float brightness) {
		Log.d(Globals.TAG, String.format("setBrightness called, brightness = %f",
				brightness));

		if (brightness < Globals.MIN_BRIGHTNESS_F)
			brightness = Globals.MIN_BRIGHTNESS_F;

		int iBrightness = (int) (brightness * Globals.MAX_BRIGHTNESS_INT);
		if (iBrightness < Globals.MIN_BRIGHTNESS_INT)
			iBrightness = Globals.MIN_BRIGHTNESS_INT;
		if (iBrightness > Globals.MAX_BRIGHTNESS_INT)
			iBrightness = Globals.MAX_BRIGHTNESS_INT;

		Settings.System.putInt(LightMonitorService.this.getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS, iBrightness);
		Log.d(Globals.TAG, String.format("putInt with %d called.", iBrightness));
		
		BrightnessController.get().setRunningBrightness(iBrightness);

		_avLayoutParams.screenBrightness = brightness;
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		wm.updateViewLayout(_av, _avLayoutParams);

		Log.d(Globals.TAG, "setBrightness done.");
	}

	public void showNotificationIcon(boolean bShow) {
		Log.d(Globals.TAG, String.format("showNotificationIcon entering, bShow = %b", bShow));
		
		if (bShow) {
			Intent ni = new Intent(this, MainActivity.class);
			PendingIntent pi = PendingIntent.getActivity(this, 0, ni,
					PendingIntent.FLAG_CANCEL_CURRENT);

			NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
			nb.setContentIntent(pi)
					.setAutoCancel(false)
					.setSmallIcon(R.drawable.ic_launcher)
					.setWhen(System.currentTimeMillis())
					.setContentTitle(
							getResources().getString(R.string.app_name))
					.setContentText(
							getResources().getString(R.string.status_running));

			startForeground(Globals.NOTIFICATION_ID, nb.getNotification());
		} else
			stopForeground(true);
		
		Log.d(Globals.TAG, "showNotificationIcon leave");
	}
	
	public void startAlertKeepalive(boolean bStart)
	{
		Log.d(Globals.TAG, String.format("startAlertKeepalive entering, bStart = %b",  bStart));
		
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		
		if(bStart)
		{
			am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, 5 * 1000 /* each five seconds */, _piSelf);
		}
		else
		{
			am.cancel(_piSelf);
		}
		
		Log.d(Globals.TAG, "startAlertKeepalive leave");
	}
}
