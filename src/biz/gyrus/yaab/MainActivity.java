/*
 * YAAB concept proof project, (C) Gyrus Solutions, 2011
 * http://www.gyrus.biz
 * 
 */

package biz.gyrus.yaab;

import java.util.Observable;
import java.util.Observer;

import biz.gyrus.yaab.BrightnessController.ServiceStatus;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private Button _btnStart = null;
	private Button _btnStop = null;
	private CheckBox _cbAutoStart = null;
	private CheckBox _cbPersistNotification = null;
	private TextView _txtStatus = null;
	private SeekBar _sbAdjLevel = null;
	private TextView _lblManualAdj = null;
	private TextView _lblBtmComment = null;
	private ProgressBar _pbCurrent = null;
	
	private Observer _oServiceStatus = new Observer() {
		
		@Override
		public void update(Observable observable, Object data) {
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					updateControls();
				}
			});
		}
	};
	
	private Observer _oRunningBrightness = new Observer() {
		
		@Override
		public void update(Observable observable, Object data) {
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					
					_pbCurrent.setProgress(BrightnessController.get().getRunningBrightness());
					
				}
			});
		}
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setTitle(R.string.title_activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        _btnStart = (Button) findViewById(R.id.btnOn);
        _btnStop = (Button) findViewById(R.id.btnOff);
        _cbAutoStart = (CheckBox) findViewById(R.id.cbAutostart);
        _cbPersistNotification = (CheckBox) findViewById(R.id.cbNotifIcon);
        _txtStatus = (TextView) findViewById(R.id.txtStatus);
        _lblManualAdj = (TextView) findViewById(R.id.lblManualAdjustment);
        _lblBtmComment = (TextView) findViewById(R.id.lblHowToUse);
        _sbAdjLevel = (SeekBar) findViewById(R.id.sbAdjLevel);
        _pbCurrent= (ProgressBar) findViewById(R.id.pbCurrent);
        
        _pbCurrent.setMax(Globals.MAX_BRIGHTNESS_INT);
        
        _sbAdjLevel.setMax(Globals.ADJUSTMENT_RANGE_INT);
        _sbAdjLevel.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {	}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser)
				{
					BrightnessController.get().setManualAdjustment(progress - seekBar.getMax()/2);
					BrightnessController.get().updateRunningBrightness();
				}
			}
		});
        
        _btnStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(Globals.TAG, "Starting service...");
				saveManualAdjustment();
				ComponentName cn = startService(new Intent(MainActivity.this, LightMonitorService.class));
				if(cn != null)
				{
					Log.i(Globals.TAG, String.format("Service Component name: %s", cn.toShortString()));
				}
				else
					Log.i(Globals.TAG, "Can't start it!");
			}
        });
        
        _btnStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(Globals.TAG, "Stopping service...");
				stopService(new Intent(MainActivity.this, LightMonitorService.class));
			}
		});
        
        _cbAutoStart.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Log.i(Globals.TAG, String.format("Saving autostart: %b", isChecked));
				AppSettings s = new AppSettings(MainActivity.this);
				s.setAutostart(isChecked);
			}
		});
        
        _cbPersistNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Log.i(Globals.TAG, String.format("Saving persist notification: %b", isChecked));
				AppSettings s = new AppSettings(MainActivity.this);
				s.setPersistNotification(isChecked);
				
				LightMonitorService lms = LightMonitorService.getInstance();
				if(lms != null)
					lms.showNotificationIcon(isChecked && BrightnessController.get().getServiceStatus() == ServiceStatus.Running);
			}
		});
        
	}
	
	protected void saveManualAdjustment()
	{
		AppSettings as = new AppSettings(this);
		as.setAdjshift(_sbAdjLevel.getProgress() - _sbAdjLevel.getMax()/2);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		BrightnessController.get().removeServiceStatusObserver(_oServiceStatus);
        BrightnessController.get().removeRunningBrightnessObserver(_oRunningBrightness);

		saveManualAdjustment();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		AppSettings s = new AppSettings(this);
        _sbAdjLevel.setProgress(_sbAdjLevel.getMax()/2 + s.getAdjshift());
        BrightnessController.get().setManualAdjustment(s.getAdjshift());

        if(!BrightnessController.get().isLightSensorPresent(this))
		{
			_txtStatus.setText(R.string.status_nosensor);
			_txtStatus.setTextColor(getResources().getColor(R.color.StatusError));
			
			_btnStart.setEnabled(false);
			_btnStop.setEnabled(false);
			
			_cbAutoStart.setEnabled(false);
			_cbAutoStart.setChecked(false);
			
			_cbPersistNotification.setEnabled(false);
			_cbPersistNotification.setChecked(false);
			
			_sbAdjLevel.setEnabled(false);
			
			_lblBtmComment.setText(R.string.txt_nolightsensor_sorry);
			
			_pbCurrent.setVisibility(View.GONE);
		}
		else
		{
			_cbAutoStart.setEnabled(true);
			_cbAutoStart.setChecked(s.getAutostart());
			_cbPersistNotification.setEnabled(true);
			_cbPersistNotification.setChecked(s.getPersistNotification());
			_lblBtmComment.setText(R.string.txt_howto_use);

			_pbCurrent.setVisibility(View.VISIBLE);
			
			updateControls();
		}
        
        BrightnessController.get().addServiceStatusObserver(_oServiceStatus);
        BrightnessController.get().addRunningBrightnessObserver(_oRunningBrightness);
	}
	
	protected void updateControls()
	{
		final ServiceStatus ssCurrent = BrightnessController.get().getServiceStatus();
		
		_btnStart.setEnabled(ssCurrent != ServiceStatus.Running);
		_btnStop.setEnabled(ssCurrent != ServiceStatus.Stopped);
		_sbAdjLevel.setEnabled(ssCurrent == ServiceStatus.Running);
		
		if(ssCurrent == ServiceStatus.Running)
		{
			_txtStatus.setText(R.string.status_running);
			_txtStatus.setTextColor(getResources().getColor(R.color.StatusHealthy));
			_pbCurrent.setProgress(BrightnessController.get().getRunningBrightness());
		}
		if(ssCurrent == ServiceStatus.Stopped)
		{
			_txtStatus.setText(R.string.status_stopped);
			_txtStatus.setTextColor(_lblManualAdj.getTextColors().getDefaultColor());
			_pbCurrent.setProgress(0);
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater i = getMenuInflater();
		i.inflate(R.menu.activity_main, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId())
		{
		case R.id.miPreferences:
			try
			{
				Intent prefsIntent = new Intent(this, PrefsActivity.class);
				startActivity(prefsIntent);
		        Log.i(Globals.TAG, "Preferences activity started");
			} catch(Exception e)
			{
				Log.e(Globals.TAG, e.getMessage(), e);
			}
			break;
		case R.id.miCredits:
			try
			{
				Intent creditsIntent = new Intent("biz.gyrus.yaab.CREDITS");
		        startActivity(creditsIntent);
		        Log.i(Globals.TAG, "Credits activity started");
			} catch(Exception e)
			{
				Log.e(Globals.TAG, e.getMessage(), e);
			}
			
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
