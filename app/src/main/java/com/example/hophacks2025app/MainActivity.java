package com.example.hophacks2025app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final String TAG = "MainActivity";

    //------FIND UI ELEMENTS------
    //for start screen
    private ConstraintLayout startLayout;
    private Button openCameraButton;
    private Button uploadImageButton;


    //for take photo screen
    private FrameLayout takePhotoLayout;
    private JavaCameraView cameraView;
    private ImageView capturedImageView;
    private Button captureButton;
    private TextView analyzingText;
    private View centerCircleOverlay;

    //for results screen
    private ConstraintLayout resultsLayout;
    private CardView fullImageViewCard;
    private ImageView fullImageView;
    private Button tryAgainButton;
    private LinearLayout resultTextLayout;
    private TextView resultText;
    private TextView suggestionsText;

    //------END FIND UI ELEMENTS

    private Mat capturedFrame;
    private Mat mRGBA;
    private boolean isCapturing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        // -------- ASSIGN UI ELEMENTS---------

        //start screen
        startLayout = findViewById(R.id.start_layout);
        openCameraButton = findViewById(R.id.open_camera_button);
        uploadImageButton = findViewById(R.id.upload_image_button);


        //for take photo screen
        takePhotoLayout = findViewById(R.id.take_photo_layout);
        cameraView = (JavaCameraView) findViewById(R.id.camera_view);
        capturedImageView = findViewById(R.id.captured_image_view);
        captureButton = findViewById(R.id.capture_button);
        analyzingText = findViewById(R.id.analyzing_text);
        centerCircleOverlay = findViewById(R.id.center_circle_overlay);


        //for results screen
        resultsLayout = findViewById(R.id.results_layout);
        fullImageViewCard = findViewById(R.id.full_image_view_card);
        fullImageView = findViewById(R.id.full_image_view);
        resultTextLayout = findViewById(R.id.result_text_layout);
        resultText = findViewById(R.id.result_text);
        suggestionsText = findViewById(R.id.suggestions_text);
        tryAgainButton = findViewById(R.id.try_again_button);
        // -------- END ASSIGN UI ELEMENTS---------



        // Check for camera permissions
        if (allPermissionsGranted()) {
            cameraView.setCameraPermissionGranted();
            loadOpenCV();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        //link all buttons
        captureButton.setOnClickListener(v -> takePhoto());
        tryAgainButton.setOnClickListener(v -> resetApp());
        openCameraButton.setOnClickListener(v -> openCamera());


        //run reset first to show start screen
        resetApp();
    }

    private void loadOpenCV() {
        if (!OpenCVLoader.initLocal()) {
            Log.e(TAG, "OpenCV Initialization Failed!");
            Toast.makeText(this, "OpenCV Initialization Failed!", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "OpenCV Initialization Successful!");
            //cameraView.enableView();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                loadOpenCV();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // --- OpenCV Camera View Listener Methods ---

    @Override
    public void onCameraViewStarted(int width, int height) {
        //capturedFrame = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
//        if (capturedFrame != null) {
//            capturedFrame.release();
//        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();

        if (isCapturing) {
            capturedFrame = mRGBA;
            isCapturing = false;
            runOnUiThread(() -> {
                analyzeImage(capturedFrame);
                captureButton.setEnabled(true);
            });
        }
        return mRGBA;
    }

    // --- Main Application Logic ---

    private void takePhoto() {
        captureButton.setEnabled(false);
        Toast.makeText(this, "Analyzing photo...", Toast.LENGTH_SHORT).show();
        isCapturing = true;
    }

    private void analyzeImage(Mat image) {

        // Hide camera view and show still full-screen image view
        takePhotoLayout.setVisibility(View.GONE);
        //cameraView.setVisibility(View.GONE);
        //capturedImageView.setVisibility(View.VISIBLE);
        resultsLayout.setVisibility(View.VISIBLE);
        //tryAgainButton.setVisibility(View.VISIBLE);

        //analyzingText.setVisibility(View.VISIBLE);
        //resultTextLayout.setVisibility(View.VISIBLE);

        // Show captured image on the ImageView
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(image, bmp);
            //set both image views to visible to this bitmap
            capturedImageView.setImageBitmap(bmp);
            fullImageView.setImageBitmap(bmp);

        } catch (Exception e) {
            Log.e(TAG, "Mat to Bitmap conversion failed: " + e.getMessage());
        }

//        cameraView.setVisibility(View.GONE);
//        centerCircleOverlay.setVisibility(View.GONE);
//        capturedImageView.setVisibility(View.VISIBLE);

        // Get average color of the center of the image
        //will need to fit with cnn logic
        int width = image.cols();
        int height = image.rows();
        int centerX = width / 2;
        int centerY = height / 2;
        int analysisSize = Math.min(width, height) / 4;

        Rect roi = new Rect(centerX - analysisSize / 2, centerY - analysisSize / 2, analysisSize, analysisSize);
        Mat roiMat = new Mat(image, roi);
        Scalar avgColor = Core.mean(roiMat);

        double avgR = avgColor.val[0];
        double avgG = avgColor.val[1];
        double avgB = avgColor.val[2];

        roiMat.release();

        // Simplified Jaundice detection logic based on Bili-Tool color charts
        String severity;
        String suggestions;

        double rG_ratio = avgR / avgG;

        if (rG_ratio > 1.2 && avgB < 100) {
            severity = "No Jaundice Detected";
            suggestions = "The skin tone appears normal. Continue to monitor for any changes.";
        } else if (rG_ratio >= 1.05 && rG_ratio <= 1.2 && avgB < 120) {
            severity = "Mild Jaundice";
            suggestions = "Provide frequent feedings (8-12 times a day). Place the baby in a well-lit room with indirect sunlight. Contact your pediatrician for a professional opinion.";
        } else {
            severity = "Moderate to Severe Jaundice";
            suggestions = "This requires immediate medical attention. Go to the nearest hospital or call emergency services immediately. Do not rely on home remedies.";
        }

        // Display results and show the "Try Again" button
        resultText.setText("Preliminary Assessment: " + severity);
        suggestionsText.setText(suggestions);
        //captureButton.setVisibility(View.GONE);

    }

    private void uploadImage(){
        //logic to upload image
        //then go to results screen???
        Toast.makeText(this, "File upload dialog opened", Toast.LENGTH_SHORT).show();
    }

    private void openCamera(){
        //set camera layout to visible
        takePhotoLayout.setVisibility(View.VISIBLE);

        //set start layout to invisible
        startLayout.setVisibility(View.GONE);

        //set results layout to invisible
        resultsLayout.setVisibility(View.GONE);
    }

    private void resetApp() { //used by "Try again" button in results screen
        //set start layout to visible
        startLayout.setVisibility(View.VISIBLE);

        //set camera layout to invisible
        takePhotoLayout.setVisibility(View.GONE);

        //set results layout to invisible
        resultsLayout.setVisibility(View.GONE);


//        // Reset UI to initial state
//        cameraView.setVisibility(View.VISIBLE);
//        capturedImageView.setVisibility(View.GONE);
//        centerCircleOverlay.setVisibility(View.VISIBLE);
//        resultTextLayout.setVisibility(View.GONE);
//        captureButton.setVisibility(View.VISIBLE);
//        tryAgainButton.setVisibility(View.GONE);
//        captureButton.setEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraView != null) {
            cameraView.enableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }
}
