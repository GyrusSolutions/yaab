package biz.gyrus.yaab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

public class RefreshScreen extends Activity {
	
	private Handler _h = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("YAAB", "RefreshScreen activity creating");
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_refreshscr);
		
		Intent brightnessIntent = this.getIntent(); 
        float brightness = brightnessIntent.getFloatExtra("floatBrightness", 0); 
        
        if(brightness < 0.1f)
        	brightness = 0.1f;

        Log.i("YAAB", String.format("Putting float brightness: %f", brightness));
        
        WindowManager.LayoutParams lp = getWindow().getAttributes(); 
        lp.screenBrightness = brightness; 
        getWindow().setAttributes(lp); 
        
		_h.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.i("YAAB", "RefreshScreen activity finishing");
				finish();
			}
		});
		
		Log.i("YAAB", "RefreshScreen activity created");
	}

}
