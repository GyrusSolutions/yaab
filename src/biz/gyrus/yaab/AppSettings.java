package biz.gyrus.yaab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

@SuppressLint("CommitPrefEdits")
public class AppSettings {
	
	private static final String _prefsName = "thesettings";
	private static final String _autostartName = "autoStartOnDeviceBoot";
	private static final String _adjshiftName = "adjShift";
	private static final String _persistNotifName = "persistNotification";
	private static final String _saverAppVerName = "savedByAppVersion";
	
	private Context _ctx = null;
	//private String _verName = null;
	private int _verNum = 1;
	
	public AppSettings(Context ctx)
	{
		_ctx = ctx;
		try {

			PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			//_verName = pInfo.versionName; 
			_verNum = pInfo.versionCode;
			
		} catch (NameNotFoundException e) {
			Log.e("YAAB", "Can't get package name.");
			e.printStackTrace();
		} 
	}
	
	private SharedPreferences getSP()
	{
		SharedPreferences sp = _ctx.getSharedPreferences(_prefsName, 0);
		if(sp.getInt(_saverAppVerName, 1) < _verNum)
		{
			upgradePrefs(sp);
		}
		return sp;
	}
	
	private void upgradePrefs(SharedPreferences sp)
	{
		if(sp.getInt(_saverAppVerName, 1) == 1)
		{
			SharedPreferences.Editor e = sp.edit();
			e.putInt(_adjshiftName, sp.getInt(_adjshiftName, 50) - 50);
			commitAndLog(e);
		}
	}
	
	private void commitAndLog(SharedPreferences.Editor e)
	{
		e.putInt(_saverAppVerName, _verNum);
		if(!e.commit())
		{
			Log.e("YAAB", "Failed to save settings.");
		}
	}
	
	public Boolean getAutostart()
	{
		SharedPreferences sp = getSP();
		return sp.getBoolean(_autostartName, true);
	}
	public void setAutostart(boolean bAutoStart)
	{
		SharedPreferences.Editor e = getSP().edit();
		
		e.putBoolean(_autostartName, bAutoStart);
		
		commitAndLog(e);
	}
	
	public int getAdjshift()
	{
		SharedPreferences sp = getSP();
		return sp.getInt(_adjshiftName, 0);
	}
	public void setAdjshift(int val)
	{
		SharedPreferences.Editor e = getSP().edit();
		
		e.putInt(_adjshiftName, val);
		
		commitAndLog(e);
	}
	
	public Boolean getPersistNotification()
	{
		SharedPreferences sp = getSP();
		return sp.getBoolean(_persistNotifName, Build.VERSION.SDK_INT > 10);	// offering the icon by default for Android 3.x and newer
	}
	public void setPersistNotification(boolean bPersist)
	{
		SharedPreferences.Editor e = getSP().edit();
		
		e.putBoolean(_persistNotifName, bPersist);
		
		commitAndLog(e);
	}
}
