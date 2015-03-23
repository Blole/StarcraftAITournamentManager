package objects;

public class ScreenshotMessage implements Message
{
	private static final long serialVersionUID = -4995884637905336631L;
	
	public final byte[] data;

	public ScreenshotMessage(byte[] data)
	{
		this.data = data;
	}
}
