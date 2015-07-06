import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

import javax.imageio.ImageIO;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class TestingFile{
	
	static JFrame gui_frame;
	static JPanel panel;
	static JButton button;
	static JButton button2;
	static JButton next;
	static JButton simulation;
	static JButton openWebcam;
	static JLabel title;
	static JLabel status;
	static JList list;
	static JProgressBar bar;
	static JScrollPane scroller;
	static BufferedImage image;
	static VideoCapture camera;
	static DefaultListModel output;
	static Mat reference_frame;
	static Mat comparison_frame;
	static Mat subtracted_frame;
	static Mat hsv_half_down;
	static Mat hsv_ref_bw;
	static Mat hsv_com_bw;
	static Mat hsv_ref_rw;
	static Mat hsv_com_rw;
	final static int NUMBER = 10;
	static boolean nextButtonPressed = false;
	static Font font;
	
	public static void main(String [] args) throws FontFormatException, IOException{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat mat = Mat.eye(3,  3, CvType.CV_8UC1);
		System.out.println("mat = " + mat.dump());
		reference_frame = new Mat();
		comparison_frame = new Mat();
		subtracted_frame = new Mat();
		setUp();
	}
	
	public static void setUp() throws FontFormatException, IOException{
		gui_frame = new JFrame();
		gui_frame.setSize(500, 500);
		gui_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui_frame.setTitle("DispenseVision");
		
		File file = new File("BAUHS93.ttf");
		font = Font.createFont(Font.TRUETYPE_FONT, file);
		
		title = new JLabel();
		title.setText("Dispense Product Detection");
		title.setFont(font.deriveFont(Font.PLAIN, 32));
		
		output = new DefaultListModel();
		
		list = new JList(output);
		
		scroller = new JScrollPane(list);
		
		status = new JLabel();
		status.setText("STATUS");
		status.setFont(new Font(status.getFont().getName(), Font.PLAIN, 24));
		
		panel = new JPanel();
		
		makeButtons();
		
		bar = new JProgressBar();
		bar.setSize(100, 20);
		bar.setMaximum(2000);
		bar.setMinimum(0);
		bar.setForeground(Color.BLACK);
		bar.setValue(1000);
		
		panel.add(title);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(button);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		panel.add(button2);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		panel.add(openWebcam);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		panel.add(scroller);
		panel.add(status);
		panel.add(bar);
		
		gui_frame.add(panel);
		gui_frame.setVisible(true);
	}
	
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
	 
	 public static void takeReferencePicture(){
		 camera = new VideoCapture(1);
			camera.open(0);
			if (!camera.isOpened())
				System.out.println("Not Open");
			else
				System.out.println("Open");
			camera.read(reference_frame);
			
			while(true){
				if (camera.read(reference_frame)){
					System.out.println("Frame Obtained");
					image = matToBufferedImage(reference_frame);
					break;
				}
			}
			writeImage("reference_image.png", reference_frame);
		    camera.release();
	 }
	 
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
	 
	 public static double checkSimilarity(){
		 hsv_half_down = new Mat();
		 hsv_ref_bw = new Mat();
		 hsv_com_bw = new Mat();
		 hsv_ref_rw = new Mat();
		 hsv_com_rw = new Mat();
		 
		 Imgproc.cvtColor(reference_frame, hsv_ref_bw, Imgproc.COLOR_RGB2GRAY);
		 Imgproc.threshold(hsv_ref_bw, hsv_ref_bw,150,255, Imgproc.THRESH_BINARY);
		 Imgproc.cvtColor(comparison_frame, hsv_com_bw, Imgproc.COLOR_RGB2GRAY);
		 Imgproc.threshold(hsv_com_bw, hsv_com_bw,150,255, Imgproc.THRESH_BINARY);
		 
		 Imgproc.GaussianBlur(reference_frame, hsv_ref_rw, new Size(45, 45), 0);
		 Imgproc.GaussianBlur(comparison_frame, hsv_com_rw, new Size(45, 45), 0);
		 
		 Imgproc.threshold(hsv_ref_bw, hsv_ref_rw,150,255, Imgproc.THRESH_BINARY);
		 Imgproc.threshold(hsv_com_bw, hsv_com_rw,150,255, Imgproc.THRESH_BINARY);
		 
		 int erosionSize = 6;
		 
		 Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2 * erosionSize + 1, 2 * erosionSize + 1));
		 
//		 Imgproc.erode(reference_frame, hsv_ref_rw, element);
//		 Imgproc.erode(comparison_frame,  hsv_com_rw, element);
//		 
//		 writeImage("hsv_ref_bw_image.png", hsv_ref_bw);
//		 writeImage("hsv_com_bw_image.png", hsv_com_bw);
//		 writeImage("hsv_ref_rw_image.png", hsv_ref_rw);
//		 writeImage("hsv_com_rw_image.png", hsv_com_rw);
		 
		 Mat ref_hist = new Mat();
		 Mat com_hist = new Mat();
		 
		 java.util.List<Mat> matList = new LinkedList<Mat>();
		 matList.add(hsv_ref_bw);
		 Mat histogram = new Mat();
		 MatOfFloat ranges=new MatOfFloat(0,256);
		 MatOfInt histSize = new MatOfInt(255);
		 Imgproc.calcHist(
		                 matList, 
		                 new MatOfInt(0), 
		                 new Mat(), 
		                 ref_hist , 
		                 histSize , 
		                 ranges);
		 matList.remove(0);
		 matList.add(hsv_com_bw);
		 Imgproc.calcHist(
                 matList, 
                 new MatOfInt(0), 
                 new Mat(), 
                 com_hist , 
                 histSize , 
                 ranges);
		 double comVal = -1;
		 for( int i = 0; i < 4; i++ )
		   { int compare_method = i;
		     double base_base = Imgproc.compareHist( ref_hist, com_hist, compare_method );
		     if (i ==1) comVal = base_base;
		    System.out.printf( " Method [%d] Perfect, Base-Half, Base-Test(1), Base-Test(2) : %f,  \n", i, base_base);
		  }
		 
		 double base_base = Imgproc.compareHist( ref_hist, com_hist, 1);
		 if (base_base > 120){
			 status.setText("Object");
			 status.setForeground(Color.GREEN);
		 }
		 else{
			 status.setText("no Object");
			 status.setForeground(Color.RED);
		 }
		 
		 bar.setValue((int) comVal);
		 return comVal;
	 }
	 
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
	 
	 public static void makeButtons(){
		 button = new JButton();
			button.setText("Take Reference Picture   ");
			button.setSize(200, 50);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					takeReferencePicture();
					output.add(0, "Reference Picture Taken");
				}
			});
			
			button2 = new JButton();
			button2.setText("        Check for Object        ");
			button2.setSize(200, 50);
			button2.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					takeComparisonPicture();
					output.add(0, "Comparison Picture Taken");
					Core.absdiff(reference_frame, comparison_frame, subtracted_frame);
					image = matToBufferedImage(subtracted_frame);
					File outputfile = new File("subtracted_image.png");
				    try {
						ImageIO.write(image, "png", outputfile);
					} catch (IOException err) {
						// TODO Auto-generated catch block
						err.printStackTrace();
					}
				    
				    output.add(0, "Histogram Comparison: " + Math.round(checkSimilarity()));
				    
				}
			});
			
			openWebcam = new JButton();
			openWebcam.setText("          Open Webcam          ");
			openWebcam.setSize(200, 50);
			openWebcam.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					WebcamThread webcamThread = new WebcamThread();
					webcamThread.start();
					output.add(0, "Webcam Open");
				}
			});
	 }
}
