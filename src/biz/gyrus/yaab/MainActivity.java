/*
 * YAAB concept proof project, (C) Gyrus Solutions, 2011
 * http://www.gyrus.biz
 * 
 */

package biz.gyrus.yaab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private Button _btnStart = null;
	private Button _btnStop = null;
	private CheckBox _cbAutoStart = null;
	private TextView _txtStatus = null;
	
	private Handler _h = new Handler();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        _btnStart = (Button) findViewById(R.id.btnOn);
        _btnStop = (Button) findViewById(R.id.btnOff);
        _cbAutoStart = (CheckBox) findViewById(R.id.cbAutostart);
        _txtStatus = (TextView) findViewById(R.id.txtStatus);
        
        _btnStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i("YAAB", "Starting service...");
				ComponentName cn = startService(new Intent(MainActivity.this, LightMonitorService.class));
				if(cn != null)
				{
					Log.i("YAAB", String.format("Service Component name: %s", cn.toShortString()));
				}
				else
					Log.i("YAAB", "Can't start it!");
				
				int brightnessMode = 0;
				try {
					 brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
				} catch (SettingNotFoundException e) {
					 // TODO Auto-generated catch block
					 e.printStackTrace();
				}
				
				if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) 
				{
				    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				}
				
				_h.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						updateStatus();
					}
				}, 500);
			}
        });
        
        _btnStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i("YAAB", "Stopping service...");
				stopService(new Intent(MainActivity.this, LightMonitorService.class));
				
				_h.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						updateStatus();
					}
				}, 500);
			}
		});
        
        _cbAutoStart.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				AppSettings s = new AppSettings(MainActivity.this);
				s.setAutostart(isChecked);
			}
		});
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		AppSettings s = new AppSettings(this);
		_cbAutoStart.setChecked(s.getAutostart());
	}
	
	protected void updateStatus()
	{
		if(LightMonitorService.getInstance() != null)
			_txtStatus.setText(R.string.status_running);
		else
			_txtStatus.setText(R.string.status_stopped);
	}
    
}
