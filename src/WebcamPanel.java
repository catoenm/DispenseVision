import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class WebcamPanel extends JPanel{
	
	public WebcamPanel(){
		
	}
	
	protected void repaint(Graphics g, BufferedImage image){
		g.drawImage(image, 0, 0, null);
	}
}
