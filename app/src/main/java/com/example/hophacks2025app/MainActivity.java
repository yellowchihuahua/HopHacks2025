package com.example.hophacks2025app;

import static org.opencv.android.Utils.matToBitmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hophacks2025app.ml.JaundiceModelFp32;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE_SELECT_IMAGE = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final String TAG = "MainActivity";
    int _MLdimension = 224; //so image size to use for cnn is 224x224
    float _MLtolerance = 0.0005f; //tolerance value, if confidence is higher than this its jaundice


    enum Subject {
        FullBody, Eyes, Feet
    }
    private Subject _currentSubject = Subject.FullBody;
    private Bitmap _bodyBitmap = null;
    private Bitmap _eyesBitmap = null;
    private Bitmap _feetBitmap = null;

    //------FIND UI ELEMENTS------
    //for start screen
    private ConstraintLayout startLayout;
    private ImageView bodyIconImgView;
    private ImageView openCameraBodyButton;
    private ImageView uploadImageBodyButton;
    private ImageView eyesIconImgView;
    private ImageView openCameraEyesButton;
    private ImageView uploadImageEyesButton;
    private ImageView feetIconImgView;
    private ImageView openCameraFeetButton;
    private ImageView uploadImageFeetButton;
    private Button analyzeImageButton;



    //for take photo screen
    private FrameLayout takePhotoLayout;
    private JavaCameraView cameraView;
    private Button captureButton;
    private TextView analyzingText;

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
        bodyIconImgView = findViewById(R.id.body_icon_img_view);
        openCameraBodyButton = findViewById(R.id.open_camera_body_button);
        uploadImageBodyButton = findViewById(R.id.upload_image_body_button);
        eyesIconImgView = findViewById(R.id.eyes_icon_img_view);
        openCameraEyesButton = findViewById(R.id.open_camera_eyes_button);
        uploadImageEyesButton = findViewById(R.id.upload_image_eyes_button);
        feetIconImgView = findViewById(R.id.feet_icon_img_view);
        openCameraFeetButton = findViewById(R.id.open_camera_feet_button);
        uploadImageFeetButton = findViewById(R.id.upload_image_feet_button);
        analyzeImageButton = findViewById(R.id.analyze_image_button);


        //for take photo screen
        takePhotoLayout = findViewById(R.id.take_photo_layout);
        cameraView = (JavaCameraView) findViewById(R.id.camera_view);
        captureButton = findViewById(R.id.capture_button);
        analyzingText = findViewById(R.id.analyzing_text);


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

        openCameraBodyButton.setOnClickListener(v -> openBodyCamera());
        openCameraEyesButton.setOnClickListener(v -> openEyesCamera());
        openCameraFeetButton.setOnClickListener(v -> openFeetCamera());
        uploadImageBodyButton.setOnClickListener(v -> uploadBodyImage());
        uploadImageEyesButton.setOnClickListener(v -> uploadEyesImage());
        uploadImageFeetButton.setOnClickListener(v -> uploadFeetImage());
        analyzeImageButton.setOnClickListener(v -> analyzeImageButtonPressed());


        captureButton.setOnClickListener(v -> takePhoto());
        uploadImageBodyButton.setOnClickListener(v -> uploadImage());
        tryAgainButton.setOnClickListener(v -> newAnalysis());

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
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();

        if (isCapturing) {
            capturedFrame = mRGBA;
            switch (_currentSubject) {
                case FullBody:
                    _bodyBitmap = matToSquareBitmap(capturedFrame);
                    break;
                case Eyes:
                    _eyesBitmap = matToSquareBitmap(capturedFrame);
                    break;
                case Feet:
                    _feetBitmap = matToSquareBitmap(capturedFrame);
                    break;
            }
            isCapturing = false;
            runOnUiThread(() -> resetApp());
        }
        return mRGBA;
    }

    // --- Main Application Logic ---

    private void takePhoto() {
        captureButton.setEnabled(false);
        Toast.makeText(this, "Photo Taken!", Toast.LENGTH_SHORT).show();
        isCapturing = true;
    }

    private Bitmap matToSquareBitmap(Mat image){
        //convert image to bmp, full res for display, square for results screen
        Bitmap squareBmp = null; //square ver results screen
        Mat centerSquareMat = null;
        try {
            int width = image.width();
            int height = image.height();
            int sideLength = Math.min(width, height);
            // 2. Calculate the starting coordinates (top-left corner) for the center square
            int x = (width - sideLength) / 2;
            int y = (height - sideLength) / 2;
            //create a submatrix (ROI region of interest) for center square
            Rect roi = new Rect(x, y, sideLength, sideLength);
            //use the ROI to create a new Mat that is square!
            centerSquareMat = new Mat(image, roi);
            if (centerSquareMat.width() > 0 && centerSquareMat.height() > 0) {
                squareBmp = Bitmap.createBitmap(centerSquareMat.width(), centerSquareMat.height(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(centerSquareMat, squareBmp);
            } else {
                Log.e("BitmapUtils", "Center square Mat has invalid dimensions after ROI creation.");
                // This case should ideally not be hit if the original image dimensions are valid
                // and sideLength is calculated correctly.
            }

            //set results image view to full resolution square
            fullImageView.setImageBitmap(squareBmp);

        } catch (Exception e) {
            Log.e(TAG, "Mat to Bitmap conversion failed: " + e.getMessage());
        }
        return squareBmp;
    }

    //sourceView is 0 for camera, 1 for files
    private void analyzeImages(Bitmap squareBmp) {

        //now, show still fullscreen image view on top of the camera, and Analyzing text
        analyzingText.setVisibility(View.VISIBLE);

        // Finally, hide camera view
        cameraView.setVisibility(View.GONE);


        //now that the display parts are done, lets run the ML on it
        //we use squareBmp from before, and downsize
        Bitmap square224Bmp = null;
        square224Bmp = Bitmap.createScaledBitmap(squareBmp, _MLdimension, _MLdimension,false);

        boolean hasJaundice = classifyImageIfJaundice(square224Bmp);


        ////-----set jaundice severity and suggestions text
        // Simplified Jaundice detection logic based on Bili-Tool color charts
        String result;
        String suggestions;

        if (!hasJaundice) {
            result = "No Jaundice Detected";
            suggestions = "Your baby does not appear to have jaundice, and their skin tone appears normal. Please continue to monitor for any changes.";
        } else {
            result = "Jaundice Detected";
            suggestions = "Your baby has signs pointing towards jaundice. We recommend seeking medical attention for a confirmatory diagnosis.";
        }



        // Display resultsLayout and show the "Try Again" button
        takePhotoLayout.setVisibility(View.GONE);
        startLayout.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        resultText.setText("Preliminary Assessment: " + result);
        suggestionsText.setText(suggestions);
        //captureButton.setVisibility(View.GONE);

    }

    public boolean classifyImageIfJaundice(Bitmap image){

        //according to https://www.youtube.com/watch?v=yV9nrRIC_R0&ab_channel=IJApps
        try {
            JaundiceModelFp32 model = JaundiceModelFp32.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, _MLdimension, _MLdimension, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * _MLdimension * _MLdimension * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[_MLdimension*_MLdimension];
            image.getPixels(intValues, 0, image.getWidth(), 0,0, image.getWidth(), image.getHeight());

            int pixel=0;

            //iterate over each pixel and extract RGB values. add those values to byte buffer
            for (int i = 0; i < _MLdimension; i++){
                for (int j = 0; j < _MLdimension; j++){
                    int val = intValues[pixel++]; //RGB together
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val  & 0xFF) * (1.f / 255));

                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            JaundiceModelFp32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();





            ///----assumed there was 2 categories, jaundice and no jaundice
//            //only 2 categories, pos 0 is normal, pos 1 is jaundice
//            float[] confidences = outputFeature0.getFloatArray();
//            Log.d(TAG, "confidences array: " + Arrays.toString(confidences));
//
//            if (confidences[1] > confidences[0]){ //if jaundice val greater than normal val
//                //Releases model resources if no longer used
//                model.close();
//                return true; //return true, jaundice detected
//            } //otherwise, jaundice val is less than or equal to normal val
            //----------end of jaundice/nojaundice assumption


            float[] confidences = outputFeature0.getFloatArray();
            Log.d(TAG, "confidences array: " + Arrays.toString(confidences));
            if (confidences[0] > _MLtolerance) { //if jaundice val greater than tolerance
                //its positive
                // Releases model resources if no longer used.
                model.close();
                return true;
            } //otherwise, jaundice val is lower than tolerance
            //its negative



            // Releases model resources if no longer used.
            model.close();
            return false; //return false, jaundice not detected
        } catch (IOException e) {
            // TODO Handle the exception
        }
        return false;
    }
    private void analyzeImageButtonPressed(){
        Mat nonSquareImg = bitmapToMat(_bodyBitmap);
        analyzeImages(matToSquareBitmap(nonSquareImg));
    }

    private void uploadBodyImage(){
        _currentSubject = Subject.FullBody;
        uploadImage();
    }
    private void uploadEyesImage(){
        _currentSubject = Subject.Eyes;
        uploadImage();
    }
    private void uploadFeetImage(){
        _currentSubject = Subject.Feet;
        uploadImage();
    }

    private void uploadImage(){
        //logic to upload image
        //then go to results screen???

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        Toast.makeText(this, "File upload dialog opened", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                // Convert to Bitmap
                Bitmap bitmap = uriToBitmap(selectedImageUri);

                if (bitmap != null) {
                    switch (_currentSubject) {
                        case FullBody:
                            _bodyBitmap = bitmap;
                            //todo: set imageViews for body, eyes, feet
                            break;
                        case Eyes:
                            _eyesBitmap = bitmap;
                            break;
                        case Feet:
                            _feetBitmap = bitmap;
                            break;
                    }

                    resetApp();
                    // Show in results screen
                    //fullImageView.setImageBitmap(bitmap);
                    //Mat nonSquareImage = bitmapToMat(bitmap);
                    //analyzeImage(matToSquareBitmap(nonSquareImage), 1);
                }
            }
        }
    }

    private Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
        return mat;
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // For Android 9 (Pie) and above
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source);
            } else {
                // For older devices
                return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openBodyCamera(){
        _currentSubject = Subject.FullBody;
        openCameraScreen();
    }
    private void openEyesCamera(){
        _currentSubject = Subject.Eyes;
        openCameraScreen();
    }
    private void openFeetCamera(){
        _currentSubject = Subject.Feet;
        openCameraScreen();
    }

    private void openCameraScreen(){
        //set camera layout to visible
        takePhotoLayout.setVisibility(View.VISIBLE);
        cameraView.setVisibility(View.VISIBLE);
        captureButton.setEnabled(true);
        cameraView.enableView();

        analyzingText.setVisibility(View.GONE);
        //centerCircleOverlay.setVisibility(View.VISIBLE);

        //set start layout to invisible
        startLayout.setVisibility(View.GONE);

        //set results layout to invisible
        resultsLayout.setVisibility(View.GONE);
    }

    private void newAnalysis(){
        _bodyBitmap = null;
        _currentSubject = Subject.FullBody;
        _eyesBitmap = null;
        _feetBitmap = null;

        resetApp();
    }
    private void resetApp() { //used by "Try again" button in results screen

        boolean isAbleToAnalyse=true;
        Drawable okIcon = getDrawable( R.drawable.ok_icon);
        Drawable errorIcon = getDrawable(R.drawable.error_icon);
        //check if body,eyes,feet bitmaps exist. set icons accordingly
        if (_bodyBitmap != null) {
            bodyIconImgView.setImageDrawable(okIcon);
        } else {
            isAbleToAnalyse=false;
            bodyIconImgView.setImageDrawable(errorIcon);
        }
        if (_eyesBitmap != null) {
            eyesIconImgView.setImageDrawable(okIcon);
        } else {
            isAbleToAnalyse=false;
            eyesIconImgView.setImageDrawable(errorIcon);
        }
        if (_feetBitmap != null) {
            feetIconImgView.setImageDrawable(okIcon);
        } else {
            isAbleToAnalyse=false;
            feetIconImgView.setImageDrawable(errorIcon);
        }

        //check if the body, eyes, feet bitmaps are null, then determine whether analyze button is enabled
        if (isAbleToAnalyse) {
            analyzeImageButton.setEnabled(true);
        } else {
            analyzeImageButton.setEnabled(false);
        }


        //set start layout to visible
        startLayout.setVisibility(View.VISIBLE);


        //set camera layout to invisible
        takePhotoLayout.setVisibility(View.GONE);

        //set results layout to invisible
        resultsLayout.setVisibility(View.GONE);


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
