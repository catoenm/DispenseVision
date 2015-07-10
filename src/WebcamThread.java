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
	static JPanel [] subPanel;
	static JLabel label, status, title, comparison, dark_comparison;
	static BufferedImage image, image2, image3;
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
	
	
	@Override
	public void run() {
		VideoCapture webCam = new VideoCapture(0);
		webcamImage = new Mat();
		refimage = new Mat();
		
		if (webCam.isOpened()){
			TestingFile.output.add(0, "Webcam Running, Don't take pictures till closed");
			count = 0;
			while(run){
				webCam.read(webcamImage);
				if (!webcamImage.empty()){
					TestingFile.comparison_frame = webcamImage;
					
					if(taking){
						System.out.println("Taking");
						taking = false;
						webCam.read(refimage);
						TestingFile.reference_frame = refimage;
					}
					
					checkSimilarityRT();
					image = TestingFile.matToBufferedImage(TestingFile.hsv_com_bw);
					ImageIcon icon = new ImageIcon(getScaledImage(image, 320, 240));
					label.setIcon(icon);
					
					image2 = TestingFile.matToBufferedImage(TestingFile.hsv_com_rw);
					ImageIcon icon2 = new ImageIcon(getScaledImage(image2, 320, 240));
					comparison.setIcon(icon2);
					
					image3 = TestingFile.matToBufferedImage(TestingFile.hsv_com_dark);
					ImageIcon icon3 = new ImageIcon(getScaledImage(image3, 320, 240));
					dark_comparison.setIcon(icon3);
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
		frame.setSize(1000, 500);
		frame.setTitle("Webcam");
		frame.setLocation(new Point(500, 0));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    frame.addWindowListener(new WindowAdapter() {
	        @Override
	        public void windowClosing(WindowEvent event) {
	            exitProcedure();
	        }
	    });
	    
	    takeRefPic = new NebraskaButton("Take Reference Picture", 300);
	    takeRefPic.setText("Take Reference Picture");
	    takeRefPic.addActionListener(new ActionListener(){
	    	public void actionPerformed(ActionEvent e){
	    		taking = true;
			}
	    });
	    
	    takeRefPic.setFont(TestingFile.font.deriveFont(Font.PLAIN, 18));
	    
	    title = new JLabel();
		title.setText("Real Time Detection");
		title.setFont(TestingFile.font.deriveFont(Font.PLAIN, 32));
	    
		label = new JLabel();
//		label.setText("Hello");
		comparison = new JLabel();
		dark_comparison = new JLabel();
		status = new JLabel();
		status.setFont(TestingFile.font.deriveFont(Font.PLAIN, 18));
		status.setText("STATUS");
		status.setFont(new Font(status.getFont().getName(), Font.PLAIN, 24));
		
		subPanel = new JPanel [10];
		for (int i = 0; i < 10; i++){
			subPanel[i] = new JPanel();
		}
		subPanel[0].add(comparison);
		subPanel[0].add(label);
		subPanel[0].add(dark_comparison);
		subPanel[1] = new JPanel();
		subPanel[1].add(title);
		subPanel[2].add(takeRefPic);
		subPanel[3].add(status);
		
		panel = new WebcamPanel();
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(subPanel[1]);
		panel.add(subPanel[2]);
		panel.add(subPanel[0]);
		panel.add(subPanel[3]);
		frame.add(panel);
	}
	
	public void exitProcedure(){
		run = false;
		
		frame.dispose();
	}
	
	public void checkSimilarityRT(){
		similarity = TestingFile.checkSimilarity();
		
		status.setText(TestingFile.status.getText());
		
		if (similarity < 150)
			status.setForeground(Color.RED);
		else
			status.setForeground(Color.GREEN);
	}
	
	public void trackImage(){
		FeatureDetector fd;
		Mat refDes;
		Mat comDes;
		DescriptorExtractor extractor;
		MatOfDMatch matches, matchesFiltered;
		DescriptorMatcher matcher;
		DMatch [] matchesList;
		ArrayList <DMatch>bestMatchesList;
		
		MatOfKeyPoint mkpRef = new MatOfKeyPoint();
		MatOfKeyPoint mkpCom = new MatOfKeyPoint();
		fd = FeatureDetector.create(FeatureDetector.BRISK);
		refDes = new Mat();
		comDes = new Mat();
		extractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
		matches = new MatOfDMatch();
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
		matchesFiltered = new MatOfDMatch();
		
		bestMatchesList = new ArrayList<DMatch>();
		
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
		
		matchesList = matches.toArray();
		
		maxDist = (double) matchesList[0].distance;
		minDist = (double) matchesList[0].distance;
		
		for (int i = 1; i < matchesList.length; i++){
			Double dist = (double) matchesList[i].distance;
			if (dist < minDist && dist != 0)
				minDist = dist;
			if (dist > maxDist)
				maxDist = dist;
		}
		
		System.out.println("Max Distance: " + maxDist);
		System.out.println("Min Distance: " + minDist);
		
		double threshold = 3 * minDist;
	    double threshold2 = 2 * minDist;
		
		if (threshold > 75)
	        threshold  = 75;
	    else if (threshold2 >= maxDist)
	        threshold = minDist * 1.1;
	    else if (threshold >= maxDist)
	        threshold = threshold2 * 1.4;
		
		System.out.println("Threshold: " + threshold);
		
		for (DMatch match : matchesList){
			Double dist = (double)match.distance;
			
			if (dist < threshold)
				bestMatchesList.add(match);
		}
		
		matchesFiltered.fromList(bestMatchesList);
		
		System.out.println("Matches Filtered: " + matchesFiltered.size());
		
		if (matchesFiltered.rows() >= 4){
			status.setText("Object Present");
			count = 0;
		} 
		else
			count++;
		
		if(count >= 4)
			status.setText("No object present");
		
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
