package com.example.hophacks2025app;



import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//from https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;


import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.*;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier faceCascade;

    //front camera id
    int frontCamera = CameraBridgeViewBase.CAMERA_ID_FRONT;
    Mat mRGBA, mRGBAT, mGray; //to flip camera display

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

// from this https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html
        if (OpenCVLoader.initLocal()) {
            Log.i("LOADED", "OpenCV loaded successfully");
        } else {
            Log.e("LOADED", "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        //set this to the camera view in the xml
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);


        //set to front camera
        mOpenCvCameraView.setCameraIndex(frontCamera);

        mOpenCvCameraView.setMaxFrameSize(2400 , 1017);

        //enable visibliity
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        loadCascade();
    }

    private void loadCascade() {
        try {
            // Load cascade from raw resources
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = getDir("cascade", MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "custom_cat_cascade.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            faceCascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (faceCascade.empty()) {
                Log.e("OpenCV", "Failed to load cascade classifier");
                faceCascade = null;
            } else {
                Log.d("OpenCV", "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

            cascadeDir.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBAT = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // code for the front camera to display flipped in color
        mRGBA = inputFrame.rgba();
        Core.flip(mRGBA, mRGBAT, 1); // mirror front camera
        mRGBA.release();

        //now, our mRGBAT is the correct flipped version of frontal camera


        mGray = inputFrame.gray();
        Core.flip(mGray,mGray,1); //mirror so its correct

        if (faceCascade != null) {
            MatOfRect faces = new MatOfRect();
            faceCascade.detectMultiScale(
                    mGray,
                    faces,
                    1.1,    // scale factor8
                    3,      // min neighbors
                    0,      // flags
                    new Size(30, 30), // min size
                    new Size()        // max size (0 = unlimited)
            );

            //use Imgproc to draw rectangles on detected faces
            for (Rect rect : faces.toArray()) {
                Imgproc.rectangle(mRGBAT, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 3);
            }
        }

        return mRGBAT;
    }
}