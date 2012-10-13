package biz.gyrus.yaab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {

		AppSettings s = new AppSettings(arg0);
		if(s.getAutostart())
		{
			Intent srvIntent = new Intent(arg0, LightMonitorService.class);
			arg0.startService(srvIntent);
		}
	}

}
