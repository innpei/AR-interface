package com.example.test2;

import androidx.appcompat.app.AppCompatActivity;

//import com.chaquo.python.PyObject;
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //TextView textView;
    //implements CameraBridgeViewBase.CvCameraViewListener2
    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat mRgba, imgGray, img;


    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch(status)
            {
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    static
    {

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
//        if (! Python.isStarted())
//            Python.start(new AndroidPlatform(this));
//
//        Python py = Python.getInstance();
//        PyObject pyf = py.getModule("hi"); //here write
//        PyObject obj = pyf.callAttr("test");
//        if (obj == null)
//            System.out.println("NULL");
//        textView = findViewById(R.id.text);
//
//        //now set text
//        assert obj != null;
//        textView.setText(obj.toString());
//        System.out.println(obj.toString());

        javaCameraView = (JavaCameraView) findViewById(R.id.my_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "openCV is Configured successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        else {
            Log.d(TAG, "openCV not working");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION ,this,mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width, CvType.CV_8UC4);
        imgGray = new Mat(height,width, CvType.CV_8UC1);
        img = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba,img,Imgproc.COLOR_RGB2BGR);
        Imgproc.cvtColor(mRgba, imgGray, Imgproc.COLOR_RGB2GRAY);
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

        for(int i=d.length-1;i>=d.length-21;--i)
        {
            Imgproc.circle(img,new Point(d[i][0],d[i][1]),4,new Scalar(0,0,255),2);
        }
        return  img;
    }

}

//        //byte pixel_at_x_y = bytes[ channels * (y * mat.cols() + x) ];