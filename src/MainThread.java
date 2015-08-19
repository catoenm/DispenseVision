import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.MatOfPoint;

public class MainThread{
	
	///////////////////////////////////////////////////////////////////
	//		Outer scope declarations
	///////////////////////////////////////////////////////////////////
	
	static double boundaries [][] = {
			{0, 15, 130}, {120, 120, 220},
			{86, 31, 4}, {220, 88, 50},
			{25, 146, 190}, {62, 174, 250},
	};
	
	static JFrame gui_frame;
	static JPanel panel;
	static JPanel subPanel [];
	static NebraskaButton button;
	static NebraskaButton button2;
	static NebraskaButton openWebcam;
	static JLabel title;
	static JLabel status;
	static JList list;
	static JProgressBar bar;
	static JScrollPane scroller;
	static BufferedImage image;
	static VideoCapture camera;
	static DefaultListModel output;
	static Mat comparison_frame, red_frame, blue_frame, yellow_frame;
	final static int NUMBER = 10;
	static boolean nextButtonPressed = false;
	static Font font;
	static ColorBlobDetector mDetector;
	static int dots, refDots = 0;
	static List<MatOfPoint> contours;
	
	public static void main(String [] args) throws FontFormatException, IOException{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat mat = Mat.eye(3,  3, CvType.CV_8UC1);
		System.out.println("mat = " + mat.dump());
		comparison_frame = new Mat();
		red_frame = new Mat();
		blue_frame = new Mat();
		yellow_frame = new Mat();
		setUp();
		output.add(0, "Initialized");
		mDetector = new ColorBlobDetector();
	}
	
	///////////////////////////////////////////////////////////////////
	//		Setting up Frame and some variables
	///////////////////////////////////////////////////////////////////
	
	public static void setUp() throws FontFormatException, IOException{
		gui_frame = new JFrame();
		gui_frame.setSize(500, 500);
		gui_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui_frame.setTitle("DispenseVision");
		
		File file = new File("calibrib.ttf");
		font = Font.createFont(Font.TRUETYPE_FONT, file);
		
		title = new JLabel();
		title.setText("Dispense Product Detection");
		title.setFont(font.deriveFont(Font.PLAIN, 32));
		title.setForeground(Color.red);
		
		output = new DefaultListModel();
		
		list = new JList(output);
		
		scroller = new JScrollPane(list);
		
		status = new JLabel();
		status.setText("STATUS");
		status.setFont(font.deriveFont(Font.PLAIN, 24));
		
		panel = new JPanel();
		
		makeButtons();
		
		bar = new JProgressBar();
		bar.setSize(100, 20);
		bar.setMaximum(2000);
		bar.setMinimum(0);
		bar.setForeground(Color.BLACK);
		bar.setValue(1000);
		
		subPanel = new JPanel [10];
		for (int i = 0; i < 10; i++){
			subPanel [i] = new JPanel();
			subPanel [i].setBackground(new Color(51, 51, 51));
		}
		
		subPanel[0].add(title);
		subPanel[3].add(button2);
		subPanel[2].add(openWebcam);
		subPanel[5].add(status);
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setOpaque(true);
		
		panel.add(subPanel[0]);
		panel.add(subPanel[3]);
		panel.add(subPanel[2]);
		panel.add(scroller);
		panel.add(subPanel[5]);
		panel.add(bar);
		
		panel.setBackground(Color.WHITE);
		panel.repaint();
		
		gui_frame.getContentPane().setBackground(Color.WHITE);
		gui_frame.add(panel);
		gui_frame.setVisible(true);
	}
	
	///////////////////////////////////////////////////////////////////
	//		Converts a mat to a buffered image and returns it
	///////////////////////////////////////////////////////////////////
	
	 public static BufferedImage matToBufferedImage(Mat matrix) {  
	     int cols = matrix.cols();  
	     int rows = matrix.rows();  
	     int elemSize = (int)matrix.elemSize();  
	     byte[] data = new byte[cols * rows * elemSize];  
	     int type;  
	     matrix.get(0, 0, data);  
	     switch (matrix.channels()) {  
	       case 1:  
	         type = BufferedImage.TYPE_BYTE_GRAY;  
	         break;  
	       case 3:  
	         type = BufferedImage.TYPE_3BYTE_BGR;  
	         // bgr to rgb  
	         byte b;  
	         for(int i=0; i<data.length; i=i+3) {  
	           b = data[i];  
	           data[i] = data[i+2];  
	           data[i+2] = b;  
	         }  
	         break;  
	       default:  
	         return null;  
	     }  
	     BufferedImage image2 = new BufferedImage(cols, rows, type);
	     image2.getRaster().setDataElements(0, 0, cols, rows, data);
	     return image2;  
	   }
	 
	 ///////////////////////////////////////////////////////////////////
	 //		Opens Camera and Takes a single Reference Picture
	 ///////////////////////////////////////////////////////////////////

	 public static void takeReferencePicture(){
		 camera = new VideoCapture(1);
		 camera.open(0);
		 if (!camera.isOpened())
			 System.out.println("Not Open");
		 else
			 System.out.println("Open");
		 camera.read(comparison_frame);

		 while(true){
			 if (camera.read(comparison_frame)){
				 System.out.println("Frame Obtained");
				 image = matToBufferedImage(comparison_frame);
				 break;
			 }
		 }
		 writeImage("reference_image.png", comparison_frame);
		 camera.release();
	 }

	 ///////////////////////////////////////////////////////////////////
	 //		Opens Camera and takes one Comparison picture
	 ///////////////////////////////////////////////////////////////////
	 
	 public static void takeComparisonPicture(){
		 camera = new VideoCapture(1);
			camera.open(0);
			if (!camera.isOpened())
				System.out.println("Not Open");
			else
				System.out.println("Open");
			camera.read(comparison_frame);
			
			while(true){
				if (camera.read(comparison_frame)){
					System.out.println("Frame Obtained");
					image = matToBufferedImage(comparison_frame);
					break;
				}
			}
			writeImage("comparison_image.png", comparison_frame);
		    camera.release();
	 }
	
	 ///////////////////////////////////////////////////////////////////
	 //		Writes any image to a file
	 ///////////////////////////////////////////////////////////////////
	 
	 public static void writeImage(String path, Mat mat){
		 
		 image = matToBufferedImage(mat);
		 
		 File outputfile = new File(path);
		 try {
			 ImageIO.write(image, "png", outputfile);
		 } catch (IOException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
		 
	 }
	 
	 ///////////////////////////////////////////////////////////////////
	 //		Makes a Histogram from a mat
	 ///////////////////////////////////////////////////////////////////
	 
	 public static Mat histogram(Mat img,Mat out) // zwraca histogram obrazu
	 {
	     Mat src = new Mat(img.height(), img.width(), CvType.CV_8UC2);
	     Vector<Mat> bgr_planes = new Vector<Mat>();                                                                                                                                                                                 
	     Core.split(src, bgr_planes);
	     MatOfInt histSize = new MatOfInt(256);
	     final MatOfFloat histRange = new MatOfFloat(0f, 256f);
	     boolean accumulate = false;
	     Mat b_hist = new  Mat();
	     Imgproc.calcHist(bgr_planes, new MatOfInt(0),new Mat(), b_hist, histSize, histRange, accumulate);
	     //writeImage("C:/users/mcatoen/desktop/pictures/histogeam.png", b_hist);
	     return b_hist;
	 }
	 
	 ///////////////////////////////////////////////////////////////////
	 //		Initializes all buttons and their click listening
	 ///////////////////////////////////////////////////////////////////
	 
	 public static void makeButtons(){
			
			button2 = new NebraskaButton("Take Picture", 300);
			button2.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					takeComparisonPicture();
					output.add(0, "Picture Taken");
				    
				}
			});
			
			button2.setFont(font.deriveFont(Font.PLAIN, 18));
			
			openWebcam = new NebraskaButton("Real Time Recognition", 300);
			openWebcam.setText("Open Webcam");
			openWebcam.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					WebcamThread webcamThread = new WebcamThread();
					webcamThread.start();
					output.add(0, "Webcam Open");
				}
			});
			
			openWebcam.setFont(font.deriveFont(Font.PLAIN, 18));
	 }
	 
	 public static int detectRed(Mat input){
		 Scalar lower = new Scalar(boundaries[0]);
		 Scalar upper = new Scalar(boundaries[1]);
		 Core.inRange(input, lower, upper, red_frame);
//		 Imgproc.cvtColor(red_frame, red_frame, Imgproc.COLOR_GRAY2RGB);
//		 Imgproc.GaussianBlur(red_frame, red_frame, new Size(3, 3), 0, 0);
//		 Core.bitwise_and(input, red_frame, red_frame);
//		 mDetector.process(red_frame);
//		 System.out.println(contours.size());
//		 Imgproc.drawContours(red_frame, contours, -1, new Scalar(255, 0, 0, 255));
		 Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
	        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7,7));
	        Imgproc.erode(red_frame, red_frame, erode);
	        Imgproc.dilate(red_frame, red_frame, dilate);
		 
		 contours = new ArrayList<>();
		 
		 Mat mat = new Mat();
	        Imgproc.findContours(red_frame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
	        Imgproc.drawContours(red_frame, contours, -1, new Scalar(255,255,0));
	        
	        System.out.println(contours.size());
		 return contours.size();
	 }
	 public static Mat detectYellow(Mat input){
		 Mat output = new Mat();
		 return output;
	 }
	 public static Mat detectGreen(Mat input){
		 Mat output = new Mat();
		 return output;
	 }
}
