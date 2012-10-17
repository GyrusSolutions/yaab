package biz.gyrus.yaab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {

		Log.i("YAAB", "Device boot broadcast received.");
		
		AppSettings s = new AppSettings(arg0);
		if(s.getAutostart())
		{
			Log.i("YAAB", "Autostart configured, starting service.");
			Intent srvIntent = new Intent(arg0, LightMonitorService.class);
			arg0.startService(srvIntent);
		}
	}

}
