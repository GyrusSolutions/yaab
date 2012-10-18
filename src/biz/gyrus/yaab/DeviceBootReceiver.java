package biz.gyrus.yaab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent arg1) {

		Log.i("YAAB", "Device boot broadcast received.");
		
		if(!BrightnessController.get().isLightSensorPresent(ctx))
		{
			Log.w("YAAB", "Light sensor not found, exit.");
			return;
		}
		
		AppSettings s = new AppSettings(ctx);
		if(s.getAutostart())
		{
			Log.i("YAAB", "Autostart configured, starting service.");
			
			BrightnessController.get().setManualAdjustment(s.getAdjshift());
			
			Intent srvIntent = new Intent(ctx, LightMonitorService.class);
			ctx.startService(srvIntent);
		}
	}

}
