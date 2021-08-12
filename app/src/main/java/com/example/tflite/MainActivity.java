package com.example.tflite;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.nex3z.fingerpaintview.FingerPaintView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TFLite";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private ImageProcessor mImgProcessor = new ImageProcessor();
    private Classifier mClassifier;
    public Mat mat;

    public ArrayList<RoiObject> mRoiImages = new ArrayList<RoiObject>(50);
    public StringBuilder ResultDigits = new StringBuilder("");
    public StringBuilder ProbabilityDigits = new StringBuilder("");

    @BindView(R.id.fpv_paint)
    FingerPaintView mFpvPaint;
    @BindView(R.id.tv_prediction)
    TextView mTvPrediction;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mat = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        ButterKnife.bind(this);
        init();
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @OnClick(R.id.btn_detect)
    void onDetectClick() {
        if (mClassifier == null) {
            Log.e(LOG_TAG, "onDetectClick(): Classifier is not initialized");
            return;
        } else if (mFpvPaint.isEmpty()) {
            Toast.makeText(this, R.string.please_write_a_digit, Toast.LENGTH_SHORT).show();
            return;
        }

        //Clear last result
        clear_numbers();

        Bitmap origImage = mFpvPaint.exportToBitmap();
        Bitmap bitmap = mFpvPaint.exportToBitmap();

        try {
            Mat imgToProcess = mImgProcessor.preProcessImage(bitmap);
            Bitmap.createScaledBitmap(bitmap, imgToProcess.width(), imgToProcess.height(), false);
            Bitmap.createScaledBitmap(origImage, imgToProcess.width(), imgToProcess.height(), false);
            Utils.matToBitmap(imgToProcess, bitmap);

            mRoiImages.clear();

            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Mat roiImage;

            Mat origImageMatrix = new Mat(origImage.getWidth(), origImage.getHeight(), CvType.CV_8UC3);
            Bitmap.Config conf = Bitmap.Config.ARGB_8888;

            Utils.bitmapToMat(origImage, origImageMatrix);
            Imgproc.findContours(imgToProcess, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {

                double contourArea = Imgproc.contourArea(contours.get(contourIdx));

                //If surface having a lot of small dark spots
                // Use this to filter out very small spots.
                if (contourArea < 300.0) {
                    continue;
                }
                Log.d(TAG, "CountourAra = " + contourArea);

                MatOfPoint2f approxCurve = new MatOfPoint2f();
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());

                double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(points);
                Imgproc.rectangle(origImageMatrix, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 255), 3);
                if ((rect.y + rect.height > origImageMatrix.rows()) || (rect.x + rect.width > origImageMatrix.cols())) {
                    continue;
                }

                MatOfDouble matMean = new MatOfDouble();
                MatOfDouble matStd = new MatOfDouble();
                Double mean;

                roiImage = imgToProcess.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
                int xCord = rect.x;
                Core.copyMakeBorder(roiImage, roiImage, 100, 100, 100, 100, Core.BORDER_ISOLATED);

                Size sz = new Size(Classifier.IMG_WIDTH, Classifier.IMG_HEIGHT);
                Imgproc.resize(roiImage, roiImage, sz);

                Core.meanStdDev(roiImage, matMean, matStd);
                mean = matMean.toArray()[0];

                Imgproc.threshold(roiImage, roiImage, mean, 255, Imgproc.THRESH_BINARY_INV);
                Bitmap tempImage = Bitmap.createBitmap(roiImage.cols(), roiImage.rows(), conf);
                Utils.matToBitmap(roiImage, tempImage);
                RoiObject roiObject = new RoiObject(xCord, tempImage);
                mRoiImages.add(roiObject);
            }

            // To read the digits from left to right - sort as per the X coordinates.
            Collections.sort(mRoiImages);

            //Set the max number of digits to read to 9 (arbitrarily chosen)
            int max = (mRoiImages.size() > 9) ? 9 : mRoiImages.size();
            for (int i = 0; i < max; i++) {
                RoiObject roi = mRoiImages.get(i);

                //Call Classifier method. Classifier was created using python code.
                String digit = FaNum.convert(String.valueOf(mClassifier.classify(roi.bmp).getNumber()));

                Log.i(TAG, "digit =" + digit);

                ResultDigits.append("" + digit);
            }
            mTvPrediction.setText(ResultDigits);
            Utils.matToBitmap(origImageMatrix, bitmap);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "مشکلی پیش آمد. لطفا دوباره امتحان کنید", Toast.LENGTH_SHORT).show();
            mFpvPaint.clear();
        }
    }

    @OnClick(R.id.btn_clear)
    void onClearClick() {
        mFpvPaint.clear();
        mTvPrediction.setText(R.string.empty);
        clear_numbers();
    }

    private void init() {
        try {
            mClassifier = new Classifier(this);
        } catch (IOException e) {
            Toast.makeText(this, R.string.failed_to_create_classifier, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "init(): Failed to create Classifier", e);
        }
    }

    private void clear_numbers() {
        mRoiImages.clear();
        ResultDigits.setLength(0);
        ProbabilityDigits.setLength(0);
    }
}