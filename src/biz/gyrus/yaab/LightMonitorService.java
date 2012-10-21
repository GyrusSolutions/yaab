/*
 * YAAB concept proof project, (C) Gyrus Solutions, 2011
 * http://www.gyrus.biz
 * 
 */

package biz.gyrus.yaab;

import java.util.ArrayList;

import biz.gyrus.yaab.BrightnessController.ServiceStatus;

import android.app.NotificationManager;
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
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;

public class LightMonitorService extends Service {
	
	public static final int DEFAULT_CHECK_PERIOD = 1000; // each second
	public static final int DEFAULT_CYCLES = 5;
	public static final float HIST_DELTA_THRESHOLD = 0.08f;

	private boolean _bActive = false;
	private int _iCycleCounter = 0;
	private Handler _h = new Handler();
    private SensorManager _sensorManager = null;
    private Sensor _lightSensor = null;
    
    private float _currentRunningReading = -2000f;
    private ArrayList<Float> _readings = null;
    private float _lastReading = -40000f;
    
    private ActivatorView _av = null;
    private WindowManager.LayoutParams _avLayoutParams = null;
    
    private static LightMonitorService _instance = null;
    public static LightMonitorService getInstance() { return _instance; }
    
    private BroadcastReceiver _brScrOFF = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				Log.i("YAAB", "ScreenOFF broadcast received, unregistering listeners.");
				
				if(_sensorManager != null)
					_sensorManager.unregisterListener(_listener);
				
				cancelTimer();
            } 
		}
	};
    
    private BroadcastReceiver _brScrON = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Intent.ACTION_SCREEN_ON))
            {
				Log.i("YAAB", "ScreenON broadcast received, registering listeners.");
				
				_sensorManager.registerListener(
						_listener,            
						_lightSensor,            
						SensorManager.SENSOR_DELAY_FASTEST);
            }
			
		}
	};
    
    private Runnable _timerHandler = new Runnable() {
		
		@Override
		public void run() {
			Log.i("YAAB", String.format("Timer hit, count %d.", _iCycleCounter));
			if(_readings != null)
			{
		        float currentReading = 0f;
		        for(float f : _readings)
		        {
		        	currentReading += f;
		        	_lastReading = f;	// wtf? why don't you just read last element from the collection directly???
		        }
		        currentReading /= _readings.size();
		        
		        _readings = null;
		        
		        Log.i("YAAB", "AVG reading: " + currentReading);
		        
		        float currentReadingBrightness = BrightnessController.get().getBrightnessFromReading(currentReading);
		        float currentRunningBrightness = BrightnessController.get().getBrightnessFromReading(_currentRunningReading);
		        
		        Log.i("YAAB", "ReadingBrightness: " + currentReadingBrightness);
		        Log.i("YAAB", "RunningBrightness: " + currentRunningBrightness);
		        
		        if(Math.abs(currentReadingBrightness - currentRunningBrightness) > HIST_DELTA_THRESHOLD)
		        {
					Log.i("YAAB", "Threshold defeated!");
		        	_currentRunningReading = currentReading;
		        	applyRunningReading();
		        }
			}
			else
			{
				if(_currentRunningReading != _lastReading)
				{
					_currentRunningReading = _lastReading;
		        	applyRunningReading();
				}
			}
			
			_iCycleCounter++;
			if(_iCycleCounter < DEFAULT_CYCLES)
				_h.postDelayed(_timerHandler, DEFAULT_CHECK_PERIOD);
			else
				_bActive = false;
		}
	};
    
    private SensorEventListener _listener = new SensorEventListener() {
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Log.i("YAAB", "Accuracy changed called!");
		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			if(event.sensor.getType()==Sensor.TYPE_LIGHT){    
				float currentReading = event.values[0];    
				Log.i("YAAB", String.format("Float brightness: %f", currentReading));
		        
				if(_readings == null)
					_readings = new ArrayList<Float>();
				_readings.add(currentReading);
		        
		        kickTimer();
			}
			
		}
	};
	
	public synchronized void applyRunningReading()
	{
		setBrightness(BrightnessController.get().getBrightnessFromReading(_currentRunningReading));
	}
	
	@Override
	public void onCreate() 
	{
		_instance = this;
        Log.i("YAAB", "Service onCreate() called");
        
		super.onCreate();
		
		if(BrightnessController.get().isLightSensorPresent(this))
		{
			AppSettings as = new AppSettings(this);
			showNotificationIcon(as.getPersistNotification());
			
			_sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			_lightSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	        
        	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        	if(pm.isScreenOn())
        	{
				_sensorManager.registerListener(
						_listener,            
						_lightSensor,            
						SensorManager.SENSOR_DELAY_FASTEST);
				
				Log.i("YAAB", "Listener registered");
        	}
        	else
        		Log.i("YAAB", "Screen is off, skip listener registration.");
        	
        	BrightnessController.get().setManualAdjustment(as.getAdjshift());
	
			_av = new ActivatorView(this);
			_avLayoutParams = new WindowManager.LayoutParams(0, 0, 0, 0, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.OPAQUE);
			_avLayoutParams.screenBrightness = 20f;
			
			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.addView(_av, _avLayoutParams);
			
			registerReceiver(_brScrOFF, new IntentFilter(Intent.ACTION_SCREEN_OFF));
			registerReceiver(_brScrON, new IntentFilter(Intent.ACTION_SCREEN_ON));
			
			BrightnessController.get().updateServiceStatus(ServiceStatus.Running);
		}
		
		Log.i("YAAB", "Service onCreate() finished");
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		Log.i("YAAB", "Service onStartCommand() called");
		
		if(BrightnessController.get().isLightSensorPresent(this))
		{
			int brightnessMode = 0;
			
			try {
				 brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
			} catch (SettingNotFoundException e) {
				 e.printStackTrace();
			}
			
			if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) 
			{
			    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
		}
		else
		{
			Log.w("YAAB", "Started on a device without light sensor. How could this happen at all?!!");
			stopSelf();
		}

		return super.onStartCommand(intent, flags, startId);
	};
	
	@Override
	public void onDestroy() {
		
		Log.i("YAAB", "Service onDestroy() called");
		BrightnessController.get().updateServiceStatus(ServiceStatus.Stopped);
		
		unregisterReceiver(_brScrOFF);
		unregisterReceiver(_brScrON);
		
		if(_sensorManager != null && _listener != null)
			_sensorManager.unregisterListener(_listener);
		
		cancelTimer();
		
		if(_av != null)
		{
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
	private void kickTimer()
	{
		Log.i("YAAB", "kickTimer in action");
		if(_bActive)
			return;
		
		_iCycleCounter = 0;
		_bActive = true;
		
		_h.postDelayed(_timerHandler, DEFAULT_CHECK_PERIOD);
		Log.i("YAAB", "Timer hit initiated!");
	}
	
	private void cancelTimer()
	{
		Log.i("YAAB", "cancelTimer called");
		_h.removeCallbacks(_timerHandler);
		_bActive = false;
		_readings = null;
	}

	private void setBrightness(float brightness)
	{
		Log.i("YAAB", String.format("setBrightness called, brightness = %f", brightness));
		
		if(brightness < 0.1f)
        	brightness = 0.1f;

        int iBrightness = (int)(brightness*255);
        if(iBrightness < 50)
        	iBrightness = 50;
        if(iBrightness > 255)
        	iBrightness = 255;
        
		Settings.System.putInt(LightMonitorService.this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, iBrightness);
		Log.i("YAAB", String.format("putInt with %d called.", iBrightness));
		
		_avLayoutParams.screenBrightness = brightness;
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		wm.updateViewLayout(_av, _avLayoutParams);
		
		Log.i("YAAB", String.format("refreshActivity started with %f", brightness));
	}
	
	public void showNotificationIcon(boolean bShow)
	{
		//NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(bShow)
		{
			Intent ni = new Intent(this, MainActivity.class);
			PendingIntent pi = PendingIntent.getActivity(this, 0, ni, PendingIntent.FLAG_CANCEL_CURRENT);
	
			NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
			nb.setContentIntent(pi).setAutoCancel(false)
				.setSmallIcon(R.drawable.ic_launcher)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(getResources().getString(R.string.status_running));
			
			//nm.notify(Globals.NOTIFICATION_TAG, Globals.NOTIFICATION_ID, nb.getNotification());
			startForeground(Globals.NOTIFICATION_ID, nb.getNotification());
		}
		else
		{
			//nm.cancel(Globals.NOTIFICATION_TAG, Globals.NOTIFICATION_ID);
			stopForeground(true);
		}
	}
}
