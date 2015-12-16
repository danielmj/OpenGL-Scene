package a4;

import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.awt.GLCanvas;
import java.lang.Thread;

public class Animator
{	private GLCanvas myCanvas;
	private long frameRate = 60;

	public Animator(GLCanvas inCanvas)
	{	myCanvas = inCanvas;
	}

	public void start()
	{	while(true)
		{	myCanvas.display();
			try
			{ Thread.sleep(frameRate);
			} catch(InterruptedException e) { }
		}
	}
}