package biz.gyrus.yaab;

public class BrightnessController {
	private BrightnessController(){}
	private static BrightnessController _instance = new BrightnessController();
	
	public static BrightnessController get() { return _instance; }
	
	public void updateRunningBrightness()
	{
		LightMonitorService lms = LightMonitorService.getInstance();
		if(lms != null)
		{
			lms.applyRunningReading();
		}
	}
}
