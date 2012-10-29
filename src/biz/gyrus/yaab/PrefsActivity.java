package biz.gyrus.yaab;

import biz.gyrus.yaab.BrightnessController.ServiceStatus;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class PrefsActivity extends PreferenceActivity {

	private final static String P_AUTOSTART = "autostart_preference";
	private final static String P_FOREGROUNDSERVICE = "fgservice_preference";
	private final static String P_ALWAYSFGSERVICE = "always_fgsrv_preference";
	private final static String P_ALERTKEEPALIVE = "alert_keepalive_preference";
	
	private CheckBoxPreference	p_bAutoStart;
	private CheckBoxPreference	p_bFgService;
	private CheckBoxPreference	p_bAlwaysFgService;
	private CheckBoxPreference	p_bAlertKeepalive;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.appprefs);
		
		p_bAutoStart = (CheckBoxPreference) findPreference(P_AUTOSTART);
		p_bFgService = (CheckBoxPreference) findPreference(P_FOREGROUNDSERVICE);
		p_bAlwaysFgService = (CheckBoxPreference) findPreference(P_ALWAYSFGSERVICE);
		p_bAlertKeepalive = (CheckBoxPreference) findPreference(P_ALERTKEEPALIVE);
		
		p_bAutoStart.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				Boolean bNewVal = (Boolean) newValue;
				
				AppSettings as = new AppSettings(PrefsActivity.this);
				as.setAutostart(bNewVal);
				
				return true;
			}
		});
		
		p_bFgService.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				Boolean bNewVal = (Boolean) newValue;
				
				AppSettings as = new AppSettings(PrefsActivity.this);
				as.setPersistNotification(bNewVal);
				
				LightMonitorService lms = LightMonitorService.getInstance();
				if(lms != null)
					lms.showNotificationIcon(bNewVal && BrightnessController.get().getServiceStatus() == ServiceStatus.Running);
				
				return true;
			}
		});
		
		p_bAlwaysFgService.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean bNewVal = (Boolean) newValue;
				
				AppSettings as = new AppSettings(PrefsActivity.this);
				as.setPersistAlwaysNotification(bNewVal);
				
				return true;
			}
		});
		
		p_bAlertKeepalive.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean bNewVal = (Boolean) newValue;
				
				AppSettings as = new AppSettings(PrefsActivity.this);
				as.setAlertKeepalive(bNewVal);
				
				LightMonitorService lms = LightMonitorService.getInstance();
				if(lms != null)
					lms.startAlertKeepalive(bNewVal);
				
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		loadPrefs();
		setTitle(R.string.preferences);
	}
	
	protected void loadPrefs()
	{
		AppSettings as = new AppSettings(this);
		
		p_bAutoStart.setChecked(as.getAutostart());
		p_bFgService.setChecked(as.getPersistNotification());
		p_bAlwaysFgService.setChecked(as.getPersistAlwaysNotification());
		p_bAlertKeepalive.setChecked(as.getAlertKeepalive());
	}
	
}
