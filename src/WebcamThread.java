import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.VideoCapture; 


public class WebcamThread implements Runnable{
	Thread t;
	static JFrame frame;
	static WebcamPanel panel;
	static JPanel [] subPanel;
	static JLabel label, status, title, comparison, dark_comparison, NumberOfDots;
	static BufferedImage image, image2, image3;
	static JButton takeRefNum;
	static NebraskaButton takeRefPic;
	static Graphics g;
	static boolean run;
	double similarity;
	boolean taking = false;
	static Mat webcamImage;
	Mat refimage;
	
	double maxDist;
	double minDist;
	
	int count;
	int dots;
	
	@Override
	public void run() {
		VideoCapture webCam = new VideoCapture(0);
		webcamImage = new Mat();
		refimage = new Mat();
		
		if (webCam.isOpened()){
			MainThread.output.add(0, "Webcam Running, Don't take pictures till closed");
			while(run){
				
				webCam.read(webcamImage);
				if (!webcamImage.empty()){
					
					MainThread.comparison_frame = webcamImage;
					
					dots = MainThread.detectRed(MainThread.comparison_frame);
					MainThread.detectSquares(MainThread.comparison_frame);
					
					image = MainThread.matToBufferedImage(MainThread.square_frame);
					ImageIcon icon = new ImageIcon(getScaledImage(image, 640, 480));
					label.setIcon(icon);
					
					image = MainThread.matToBufferedImage(MainThread.comparison_frame);
					icon = new ImageIcon(getScaledImage(image, 320, 240));
					dark_comparison.setIcon(icon);
					
					status.setText(dots + " Scanned");
				}
			}
		}
		webCam.release();
	}
	public void start(){
		if (t == null){
			t = new Thread(this);
			t.start();
		}
		run = true;
		setup();
	}
	
	public void setup(){
		frame = new JFrame();
		frame.setVisible(true);
		frame.setSize(750, 750);
		frame.setTitle("Webcam");
		frame.setLocation(new Point(500, 0));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    frame.addWindowListener(new WindowAdapter() {
	        @Override
	        public void windowClosing(WindowEvent event) {
	            exitProcedure();
	        }
	    });
	    
	    title = new JLabel();
		title.setText("Real Time Detection");
		title.setFont(MainThread.font.deriveFont(Font.PLAIN, 32));
		title.setForeground(Color.red);
		
		takeRefNum = new NebraskaButton("Grab Number", 300);
		takeRefNum.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
			    MainThread.refDots = dots;
			    MainThread.output.add(0,  "Reference Number Achieved: " + MainThread.refDots);
			    MainThread.output.add(0, MainThread.contours.get(0).size());
			}
		});
		
		label = new JLabel();
//		label.setText("Hello");
		comparison = new JLabel();
		dark_comparison = new JLabel();
		status = new JLabel();
		status.setFont(MainThread.font.deriveFont(Font.PLAIN, 18));
		status.setText("STATUS");
		status.setFont(new Font(status.getFont().getName(), Font.PLAIN, 24));
		status.setForeground(Color.red);
		
		subPanel = new JPanel [4];
		for (int i = 0; i < 4; i++){
			subPanel[i] = new JPanel();
			subPanel[i].setBackground(new Color(51, 51, 51));
		}
		subPanel[0].add(comparison);
		subPanel[0].add(label);
		subPanel[0].add(dark_comparison);
		subPanel[1].add(title);
		subPanel[2].add(status);
		subPanel[3].add(takeRefNum);
		
		panel = new WebcamPanel();
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(subPanel[1]);
		panel.add(subPanel[3]);
		panel.add(subPanel[0]);
		panel.add(subPanel[2]);
		frame.add(panel);
	}
	
	public void exitProcedure(){
		run = false;
		
		frame.dispose();
	}
	
	
	private BufferedImage getScaledImage(Image srcImg, int w, int h){
	    BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
	    Graphics2D g2 = resizedImg.createGraphics();
	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    g2.drawImage(srcImg, 0, 0, w, h, null);
	    g2.dispose();
	    return resizedImg;
	}
}
