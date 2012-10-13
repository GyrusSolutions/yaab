/*
 * YAAB concept proof project, (C) Gyrus Solutions, 2011
 * http://www.gyrus.biz
 * 
 */

package biz.gyrus.yaab;

import java.util.ArrayList;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

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
		        
		        float currentReadingBrightness = getBrightnessFromReading(currentReading);
		        float currentRunningBrightness = getBrightnessFromReading(_currentRunningReading);
		        
		        Log.i("YAAB", "ReadingBrightness: " + currentReadingBrightness);
		        Log.i("YAAB", "RunningBrightness: " + currentRunningBrightness);
		        
		        if(Math.abs(currentReadingBrightness - currentRunningBrightness) > HIST_DELTA_THRESHOLD)
		        {
					Log.i("YAAB", "Threshold defeated!");
		        	_currentRunningReading = currentReading;
		        	setBrightness(currentReadingBrightness);
		        }
			}
			else
			{
				if(_currentRunningReading != _lastReading)
				{
					_currentRunningReading = _lastReading;
		        	setBrightness(getBrightnessFromReading(_lastReading));
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
			// TODO Auto-generated method stub
			Log.i("YAAB", "Accuracy changed called!");
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
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
	
	@Override
	public void onCreate() 
	{
		_instance = this;
		super.onCreate();
		
		registerReceiver(_brScrOFF, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(_brScrON, new IntentFilter(Intent.ACTION_SCREEN_ON));
		
		_sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		_lightSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		
        Log.i("YAAB", "Service onCreate() called");
        
		if (_lightSensor == null){         
			Log.e("YAAB", "No light sensor present!");
        } 
        else
        {         
			//float max =  lightSensor.getMaximumRange();
			
			//lightMeter.setMax((int)max);         
			//textMax.setText("Max Reading: " + String.valueOf(max));                   
			_sensorManager.registerListener(
					_listener,            
					_lightSensor,            
					SensorManager.SENSOR_DELAY_FASTEST);
			
			Log.i("YAAB", "Listener registered");
		}

		Log.i("YAAB", "Service onCreate() finished");
        
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		Log.i("YAAB", "Service onStartCommand() called");
		return super.onStartCommand(intent, flags, startId);
	};
	
	@Override
	public void onDestroy() {
		Log.i("YAAB", "Service onDestroy() called");
		
		unregisterReceiver(_brScrOFF);
		unregisterReceiver(_brScrON);
		
		if(_sensorManager != null)
			_sensorManager.unregisterListener(_listener);
		
		cancelTimer();
		
		super.onDestroy();
		_instance = null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// must be called when incoming sensor change arrives
	private void kickTimer()
	{
		Log.i("YAAB", "kickTimer in action");
		//_h.removeCallbacks(_timerHandler);	// cancelling existing timers (what for btw?)
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
        
        Intent intent = new Intent(LightMonitorService.this, RefreshScreen.class); 
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
        intent.putExtra("floatBrightness", brightness);  
        getApplication().startActivity(intent);
		Log.i("YAAB", String.format("refreshActivity started with %f", brightness));
	}
	
	private float getBrightnessFromReading(float reading)
	{
		return (float)(14*Math.log(reading) - 38)/100f;
	}
}