package biz.gyrus.yaab;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CreditsActivity extends Activity {

	protected void onCreate(android.os.Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_credits);
		
		TextView txtMain = (TextView) findViewById(R.id.txtMain);
		txtMain.setMovementMethod(LinkMovementMethod.getInstance());
		
		Button btnClose = (Button) findViewById(R.id.btnClose);
		btnClose.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	};
}
