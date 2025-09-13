package com.example.hophacks2025app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hophacks2025app.R;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final String TAG = "MainActivity";

    private JavaCameraView cameraView;
    private ImageView capturedImageView;
    private Button captureButton;
    private Button tryAgainButton;
    private LinearLayout resultLayout;
    private TextView resultText;
    private TextView suggestionsText;
    private View centerCircleOverlay;

    private Mat capturedFrame;
    private Mat mRGBA;
    private boolean isCapturing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        // Find UI elements…
        cameraView = (JavaCameraView) findViewById(R.id.camera_view);
        capturedImageView = findViewById(R.id.captured_image_view);
        captureButton = findViewById(R.id.capture_button);
        tryAgainButton = findViewById(R.id.try_again_button);
        resultLayout = findViewById(R.id.result_layout);
        centerCircleOverlay = findViewById(R.id.center_circle_overlay);
        resultText = findViewById(R.id.result_text);
        suggestionsText = findViewById(R.id.suggestions_text);

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

        captureButton.setOnClickListener(v -> takePhoto());
        tryAgainButton.setOnClickListener(v -> resetApp());

        //run reset first
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
        // Hide camera view and show result layout
        //cameraView.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);

        // Show captured image on the ImageView
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(image, bmp);
            capturedImageView.setImageBitmap(bmp);
        } catch (Exception e) {
            Log.e(TAG, "Mat to Bitmap conversion failed: " + e.getMessage());
        }

        cameraView.setVisibility(View.GONE);
        centerCircleOverlay.setVisibility(View.GONE);
        capturedImageView.setVisibility(View.VISIBLE);

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
        captureButton.setVisibility(View.GONE);
        tryAgainButton.setVisibility(View.VISIBLE);
    }

    private void resetApp() {
        // Reset UI to initial state
        cameraView.setVisibility(View.VISIBLE);
        capturedImageView.setVisibility(View.GONE);
        centerCircleOverlay.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        tryAgainButton.setVisibility(View.GONE);
        captureButton.setEnabled(true);
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
