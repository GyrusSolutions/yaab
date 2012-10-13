package biz.gyrus.yaab;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
	
	private static final String _prefsName = "thesettings";
	private static final String _autostartName = "autoStartOnDeviceBoot";
	
	private Context _ctx = null;
	
	public AppSettings(Context ctx)
	{
		_ctx = ctx;
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
		
		if(!e.commit())
		{
			// TODO: log error
		}
	}
}
