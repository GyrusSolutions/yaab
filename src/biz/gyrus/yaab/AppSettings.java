package biz.gyrus.yaab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

@SuppressLint("CommitPrefEdits")
public class AppSettings {
	
	private static final String _prefsName = "thesettings";
	private static final String _autostartName = "autoStartOnDeviceBoot";
	private static final String _adjshiftName = "adjShift";
	
	private Context _ctx = null;
	
	public AppSettings(Context ctx)
	{
		_ctx = ctx;
	}
	
	private void commitAndLog(SharedPreferences.Editor e)
	{
		if(!e.commit())
		{
			Log.e("YAAB", "Failed to save settings.");
		}
	}
	
	public Boolean getAutostart()
	{
		SharedPreferences sp = _ctx.getSharedPreferences(_prefsName, 0);
		return sp.getBoolean(_autostartName, true);
	}
	public void setAutostart(boolean bAutoStart)
	{
		SharedPreferences.Editor e = _ctx.getSharedPreferences(_prefsName, 0).edit();
		
		e.putBoolean(_autostartName, bAutoStart);
		
		commitAndLog(e);
	}
	
	public int getAdjshift()
	{
		SharedPreferences sp = _ctx.getSharedPreferences(_prefsName, 0);
		return sp.getInt(_adjshiftName, 50);
	}
	public void setAdjshift(int val)
	{
		SharedPreferences.Editor e = _ctx.getSharedPreferences(_prefsName, 0).edit();
		
		e.putInt(_adjshiftName, val);
		
		commitAndLog(e);
	}
}
