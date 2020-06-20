package com.example.cvcamera;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class Main3Activity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static String TAG = "MainActivity3";
    ArrayList savedPoints;
    Mat mRgba,imgGray,img;
    JavaCameraView javaCameraView;

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity3_main);

        Intent intent = getIntent();
        savedPoints = intent.getParcelableArrayListExtra("points");
        System.out.println("Saved Points "+savedPoints);
        javaCameraView = (JavaCameraView) findViewById(R.id.my_camera_view2);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "openCV is Configured successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "openCV not working");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        imgGray = new Mat(height, width, CvType.CV_8UC1);
        img = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //Imgproc.cvtColor(mRgba,img,Imgproc.COLOR_RGB2BGR);
        Imgproc.cvtColor(mRgba, imgGray, Imgproc.COLOR_BGR2GRAY);
        MatOfPoint corners = new MatOfPoint();
        Mat gauss = new Mat();
        Imgproc.GaussianBlur(imgGray,gauss, new Size(3,3),0);  //Gaussian blur
        int rows = gauss.rows();
        int cols = gauss.cols();

        //converting gauss to array, creating dx and dy
        MatOfInt mint = new MatOfInt();
        gauss.convertTo(mint,CvType.CV_32S);
        int[] imgArr = new int[(int)(mint.total()*mint.channels())];
        mint.get(0,0,imgArr);
        double[] dx = new double[(int)(mint.total()*mint.channels())];
        double[] dy = new double[(int)(mint.total()*mint.channels())];

        for(int i=0;i<imgArr.length;++i)
        {
            if (i%cols == 0 || i%cols == cols-1)
                dx[i] = 0;
            else
                dx[i] = Math.abs((imgArr[i+1]-imgArr[i-1])/2.0);
            if (i<cols || i>=imgArr.length-cols)
                dy[i] = 0;
            else
                dy[i] = Math.abs((imgArr[i+cols]-imgArr[i-cols])/2.0);
        }


        //GF
        Imgproc.goodFeaturesToTrack(gauss,corners,100,0.03,13);
        Point[] p = new Point[100];
        p = corners.toArray();
        double[][] d = new double[100][3];

        for(int i=0; i<p.length;++i)
        {
            d[i][0] = p[i].x;
            d[i][1] = p[i].y;
            if (dx[(int)(d[i][1]*gauss.cols()+d[i][0])]==0 || dy[(int)(d[i][1]*gauss.cols()+d[i][0])]==0)
                d[i][2] = 0;
            else
                d[i][2] = (dx[(int)(d[i][1]*gauss.cols()+d[i][0])]+dy[(int)(d[i][1]*gauss.cols()+d[i][0])])/2.0;
        }

        //sort d
        java.util.Arrays.sort(d, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a[2], b[2]);
            }
        });

        ArrayList<ArrayList<Double>> markedPoints = new ArrayList<ArrayList<Double>>();
        for(int i=d.length-1;i>=d.length-21;--i)
        {
            //Imgproc.circle(mRgba,new Point(d[i][0],d[i][1]),4,new Scalar(0,0,255),2);
            System.out.println(d[i][0]+" "+d[i][1]);
            ArrayList<Double> point = new ArrayList<Double>();
            point.add(d[i][0]);
            point.add(d[i][1]);
            markedPoints.add(point);
        }

        ArrayList found_points = getFoundPoints(markedPoints,savedPoints.size());
        if (found_points.size() != 0)
            System.out.println("Found points "+found_points);

        for (int i=0;i<found_points.size()-1;++i){
            double x = (double)found_points.get(i);
            double y = (double)found_points.get(i+1);
            Imgproc.rectangle(mRgba,new Point(x,y),new Point(x+50,y+50),new Scalar(0,0,255),5);
            ++i;
        }

        return  mRgba;
    }


    private ArrayList<Double> getFoundPoints(ArrayList<ArrayList<Double>> detected, int n) {
        ArrayList<Double> res = new ArrayList<Double>();
        int[] visited_point = new int[n];
        for(int i=0; i<detected.size();++i)
        {
            ArrayList d = (ArrayList) detected.get(i);
            double x = (double)d.get(0);
            double y = (double)d.get(1);
            for(int j=0; j<savedPoints.size();++j)
            {
                if (visited_point[j]==0)
                {
                    ArrayList s = (ArrayList) savedPoints.get(j);
                    double pointX = (double)s.get(0);
                    double pointY = (double)s.get(1);
                    //Imgproc.circle(img,new Point(pointX,pointY),4,new Scalar(0,255,0),10);
                    //System.out.println(j+" Saved points: "+pointY+" "+pointX);
                    if (Math.abs(pointX-x)<=20 && Math.abs(pointY-y)<=20)
                    {
                        res.add(x);
                        res.add(y);
                        visited_point[j] = 1;
                    }
                }
            }
        }
        return res;
    }
}
