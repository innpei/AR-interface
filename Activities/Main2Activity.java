package com.example.cvcamera;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

class MyClass
{
    Context mContext;
    public MyClass(Context context)
    {
        mContext=context;
    }
}

public class Main2Activity extends AppCompatActivity implements Dialog.DialogListener{
    Point[] markedPoints = new Point[20];
    Button button2,save_btn;
    ImageView imageView;
    Button[] button = new Button[20];
    ArrayList<ArrayList<Double>> savedPoints = new ArrayList<ArrayList<Double>>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity2_main);

        button2 = (Button) findViewById(R.id.button2);
        save_btn = findViewById(R.id.save_btn);
        imageView = (ImageView) findViewById(R.id.imageView);

        Intent intent = getIntent();
        String filename = intent.getExtras().getString("screenshot");
        //File file = new File(filename);
        Mat mat = Imgcodecs.imread(filename);
        Mat img = ProcessImg(mat);

        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, matOfByte);
        byte[] bytes = matOfByte.toArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imageView.setImageBitmap(bitmap);
        ViewTreeObserver viewTreeObserver = imageView.getViewTreeObserver();

        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    for (int i = 0; i < 20; ++i) {
                        Button b = MakeButtons(markedPoints, i);
                        button[i] = b;
                        button[i].setTag(i);
                        button[i].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                v.setBackgroundResource(R.drawable.button_pressed);
                                int pos = Integer.parseInt(v.getTag().toString());
                                ArrayList<Double> dob = new ArrayList<Double>();
                                dob.add((double) markedPoints[pos].x);
                                dob.add((double) markedPoints[pos].y);
                                savedPoints.add(dob);
                            }
                        });
                    }

                    imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    //System.out.println(savedPoints);
                }
            });
        }

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Main2Activity.this,Main3Activity.class);
                intent.putExtra("points", savedPoints);
                startActivity(intent);
            }
        });
    }

    TextView editText;
    String desName;
    int num=0;
    private void openDialog() {
        Dialog dialog = new Dialog();
        dialog.show(getSupportFragmentManager(),"dialog");
    }

    @Override
    public void applyTexts(String desName) {
        if(!fileExists(this,desName)) {
            FileOutputStream fos = null;
            try {
                fos = openFileOutput(desName, MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(savedPoints);
                oos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(this,"saved to My Profile", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Profile.class);
            intent.putExtra("String", desName);
            startActivity(intent);
        }
        else
            Toast.makeText(this,"The File name already exists!", Toast.LENGTH_LONG).show();
    }

    public boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            return false;
        }
        return true;
    }


    double cvImgWidth,cvImgHeight;

    private Mat ProcessImg(Mat mat) {
        Mat imgGray = new Mat();
        //Mat img = new Mat();
        //Imgproc.cvtColor(mat,img,Imgproc.COLOR_RGB2BGR);
        Imgproc.cvtColor(mat, imgGray, Imgproc.COLOR_BGR2GRAY);
        MatOfPoint corners = new MatOfPoint();
        Mat gauss = new Mat();
        Imgproc.GaussianBlur(imgGray,gauss, new Size(11,11),0);  //Gaussian blur
        int rows = gauss.rows();
        int cols = gauss.cols();

        //converting gauss to array, creating dx and dy
        MatOfInt mint = new MatOfInt();
        gauss.convertTo(mint, CvType.CV_32S);
        int[] imgArr = new int[(int)(mint.total()*mint.channels())];
        mint.get(0,0,imgArr);
        double[] dx = new double[(int)(mint.total()*mint.channels())];
        double[] dy = new double[(int)(mint.total()*mint.channels())];

        System.out.println("Length "+imgArr.length);
        System.out.println(mint.rows()+" "+mint.cols());
        cvImgWidth=mint.cols();
        cvImgHeight=mint.height();

        System.out.println(mint.total());
        System.out.println(mint.channels());
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
        Imgproc.goodFeaturesToTrack(gauss,corners,100,0.03,25);
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
        Mat fin_img = new Mat();
        Imgproc.cvtColor(mat,fin_img,Imgproc.COLOR_RGB2BGR);

        for(int i=d.length-1;i>=d.length-20;--i)
        {
            Imgproc.circle(fin_img,new Point(d[i][0],d[i][1]),5,new Scalar(0,0,255),4);
            markedPoints[d.length-1-i] = new Point(d[i][0],d[i][1]);
//                    markedPoints[d.length-1-i].x = d[i][0];
//                    markedPoints[d.length-1-i].y=d[i][1];
//                    Nums[0] = (float) d[i][1]/img.height();
//                    Nums[1] = (float) d[i][0]/img.width();
            System.out.println("marked points "+markedPoints[d.length-1-i].x+" "+markedPoints[d.length-1-i].y);
            System.out.println(d[i][0]+" "+d[i][1]);
        }

        return  fin_img;
    }


    private Button MakeButtons(Point[] p,int pos){
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.mylayout);
        System.out.println("Layout size "+layout.getWidth()+" "+layout.getHeight());
        Button btnTag = new Button(this);
        btnTag.setBackgroundResource(R.drawable.button_img);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(30,30);
        System.out.println("Image view size "+imageView.getWidth()+" "+imageView.getHeight());
        double distX = Math.abs(p[pos].x-960);
        double distY=Math.abs(p[pos].y-540);
        distX = distX/18;
        distY = distY/29;
        if(p[pos].x>960 && p[pos].y<540)
        {
            System.out.println("distXY "+distX+" "+distY);
            params.leftMargin=(int)(p[pos].x/cvImgWidth*1650+75-distX);
            params.topMargin = (int)(p[pos].y/cvImgHeight*930+60+distY);
        }
        else if(p[pos].x>960 && p[pos].y>540)
        {
            params.leftMargin=(int)(p[pos].x/cvImgWidth*1650+75-distX);
            params.topMargin = (int)(p[pos].y/cvImgHeight*930+60-distY);
        }
        else if(p[pos].x<960 && p[pos].y<540)
        {
            params.leftMargin=(int)(p[pos].x/cvImgWidth*1650+75+distX);
            params.topMargin = (int)(p[pos].y/cvImgHeight*930+60+distY);
        }
        else
        {
            params.leftMargin=(int)(p[pos].x/cvImgWidth*1650+75+distX);
            params.topMargin = (int)(p[pos].y/cvImgHeight*930+60-distY);
        }
        btnTag.setLayoutParams(params);
        layout.addView(btnTag);
        return btnTag;
    }

}

