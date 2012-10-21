package biz.gyrus.yaab;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class BrightnessController {
	private BrightnessController(){}
	private static BrightnessController _instance = new BrightnessController();
	
	public static BrightnessController get() { return _instance; }
	
	public enum ServiceStatus { Error, Stopped, Running }
	
	private static class ServiceStatusObservable extends Observable
	{
		private ServiceStatus _ssCurrent = ServiceStatus.Stopped;

		public ServiceStatus getStatus() { return _ssCurrent; }
		
		public void setStatus(ServiceStatus ss)
		{
			if(ss != _ssCurrent)
				setChanged();
			
			_ssCurrent = ss;
			notifyObservers();
		}
	}
	
	private static class RunningBrightnessObservable extends Observable
	{
		private int _iRunningBrightness = 0;
		
		public int getRunningBrightness() { return _iRunningBrightness; }
		
		public void setRunningBrightness(int brightness)
		{
			if(_iRunningBrightness != brightness)
				setChanged();
			
			_iRunningBrightness = brightness;
			notifyObservers();
		}
	}
	
	private Boolean _bIsSensorPresent = null;
	private int _iManualAdjustmentValue = 0;
	private ServiceStatusObservable _oServStatus = new ServiceStatusObservable();
	private RunningBrightnessObservable _oRunningBrightness = new RunningBrightnessObservable();
	
	public boolean isLightSensorPresent(Context ctx)
	{
		if(_bIsSensorPresent == null)
		{
			SensorManager sm = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
			Sensor ls = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
			
			_bIsSensorPresent = (ls != null);
		}
		return _bIsSensorPresent;
	}
	
	public void updateRunningBrightness()
	{
		LightMonitorService lms = LightMonitorService.getInstance();
		if(lms != null)
		{
			lms.applyRunningReading();
		}
	}
	
	public int getManualAdjustment() { return _iManualAdjustmentValue; }
	public void setManualAdjustment(int val)
	{
		_iManualAdjustmentValue = val;
	}
	public float getBrightnessFromReading(float reading)
	{
		return (float)(14*Math.log(reading) - 38 + _iManualAdjustmentValue)/100f;
	}
	
	
	public void addServiceStatusObserver(Observer o)
	{
		_oServStatus.addObserver(o);
	}
	public void removeServiceStatusObserver(Observer o)
	{
		_oServStatus.deleteObserver(o);
	}
	public void updateServiceStatus(ServiceStatus ss)
	{
		_oServStatus.setStatus(ss);
	}
	public ServiceStatus getServiceStatus() { return _oServStatus.getStatus(); }
	
	
	public void addRunningBrightnessObserver(Observer o)
	{
		_oRunningBrightness.addObserver(o);
	}
	public void removeRunningBrightnessObserver(Observer o)
	{
		_oRunningBrightness.deleteObserver(o);
	}
	public void setRunningBrightness(int brightness)
	{
		_oRunningBrightness.setRunningBrightness(brightness);
	}
	public int getRunningBrightness() { return _oRunningBrightness.getRunningBrightness(); }
}
