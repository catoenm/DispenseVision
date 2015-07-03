import java.awt.Graphics;
import java.awt.List;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.videoio.VideoCapture;


public class WebcamThread implements Runnable{
	Thread t;
	static JFrame frame;
	static WebcamPanel panel;
	static JLabel label;
	static JLabel status;
	static BufferedImage image;
	static Graphics g;
	static boolean run;
	double similarity;
	FeatureDetector fd;
	MatOfKeyPoint mkpRef;
	MatOfKeyPoint mkpCom;
	Mat refDes;
	Mat comDes;
	DescriptorExtractor extractor;
	MatOfDMatch matches, matchesFiltered;
	DescriptorMatcher matcher;
	List matchesList;
	List bestMatchesList;
	
	
	
	@Override
	public void run() {
		VideoCapture webCam = new VideoCapture(0);
		Mat webcamImage = new Mat();
		if (webCam.isOpened()){
			TestingFile.output.add(0, "Webcam Running, Don't take pictures till closed");
			while(run){
				System.out.println("Hello");
				webCam.read(webcamImage);
				if (!webcamImage.empty()){
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					TestingFile.comparison_frame = webcamImage;
					
					fd.detect(TestingFile.reference_frame, mkpRef);
					fd.detect(TestingFile.comparison_frame, mkpCom);
					
					System.out.println("reference image size: " + mkpRef.size());
					System.out.println("comparison image size: " + mkpCom.size());
					
					extractor.compute(TestingFile.reference_frame, mkpRef, refDes);
					extractor.compute(TestingFile.comparison_frame, mkpCom, comDes);
					
					System.out.println("Ref Descriptor: " + refDes.size());
					System.out.println("Com Descriptor: " + comDes.size());
					
					matcher.match(refDes, comDes, matches);
					
					System.out.println("Matches: " + matches.size());
					
					
					
//					similarity = TestingFile.checkSimilarity();
					
//					if (similarity < 150000)
//						status.setText("No Object");
//					else
//						status.setText("Object Present");
//					
//					image = TestingFile.matToBufferedImage(TestingFile.comparison_frame);
//					ImageIcon icon = new ImageIcon(image);
//					label.setIcon(icon);
					
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
		
		mkpRef = new MatOfKeyPoint();
		mkpCom = new MatOfKeyPoint();
		fd = FeatureDetector.create(FeatureDetector.BRISK);
		refDes = new Mat();
		comDes = new Mat();
		extractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
		matches = new MatOfDMatch();
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
		matchesFiltered = new MatOfDMatch();
		matchesList = (List) matches.toList();
		bestMatches = new ArrayList<DMatch>();
		
		
	}
	
	public void setup(){
		frame = new JFrame();
		frame.setVisible(true);
		frame.setSize(700, 700);
		frame.setTitle("Webcam");
		frame.setLocation(new Point(500, 500));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    frame.addWindowListener(new WindowAdapter() {
	        @Override
	        public void windowClosing(WindowEvent event) {
	            exitProcedure();
	        }
	    });
		label = new JLabel();
//		label.setText("Hello");
		
		status = new JLabel();
		status.setText("STATUS");
		
		panel = new WebcamPanel();
		
		panel.add(label);
		panel.add(status);
		
		frame.add(panel);
	}
	
	public void exitProcedure(){
		run = false;
		
		frame.dispose();
	}
}
