package com.example.tflite;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

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

import java.util.ArrayList;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    // Amplify the region of interest by making the number bright and
    // background black. This removes noise due to shadows / insufficient lighting.
    public Mat preProcessImage(Bitmap image) {
        Size sz = new Size(700, 525);
        ArrayList<Rect> rects;
        Rect rect;
        int top, bottom, left, right;
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Mat origImageMatrix = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC3);
        Mat tempImageMat = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1, new Scalar(0));
        Utils.bitmapToMat(image, origImageMatrix);

        Mat imgToProcess = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1);
        Mat imgToProcessCanny = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(image, imgToProcess);

        //Resize image to manageable size
        Imgproc.resize(imgToProcess, imgToProcess, sz, 0, 0, Imgproc.INTER_NEAREST);
        Imgproc.resize(origImageMatrix, origImageMatrix, sz, 0, 0, Imgproc.INTER_NEAREST);
        Imgproc.resize(tempImageMat, tempImageMat, sz, 0, 0, Imgproc.INTER_NEAREST);
        Imgproc.cvtColor(imgToProcess, imgToProcess, Imgproc.COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(imgToProcess, imgToProcess, new Size(5, 5), 0);

        Mat imgGrayInv = new Mat(sz, CvType.CV_8UC1, new Scalar(255.0));

        //Invert the brightness - make lighter pixel dark and vice versa.
        Core.subtract(imgGrayInv, imgToProcess, imgGrayInv);
        Imgproc.Canny(imgToProcess, imgToProcessCanny, 13, 39, 3, false);

        rects = this.boundingBox(imgToProcessCanny);
        Log.d(TAG, "Length of rects : " + rects.size());

        if (rects.size() != 0) {
            rect = rects.get(0);
            top = rect.y;
            bottom = rect.y + rect.height;
            left = rect.x;
            right = rect.x + rect.height;
            for (int i = 1; i < rects.size(); i++) {
                rect = rects.get(i);
                if (rect.y < top) {
                    top = rect.y;
                }
                if (rect.y + rect.height > bottom) {
                    bottom = rect.y + rect.height;
                }
                if (rect.x < left) {
                    left = rect.x;
                }
                if (rect.x + rect.width > right) {
                    right = rect.x + rect.width;
                }
            }

            Mat aux = tempImageMat.colRange(left, right).rowRange(top, bottom);
            MatOfDouble matMean = new MatOfDouble();
            MatOfDouble matStd = new MatOfDouble();
            Double mean;
            Double std;
            Mat roiImage = imgGrayInv.submat(top, bottom, left, right).clone();
            Core.meanStdDev(roiImage, matMean, matStd);
            mean = matMean.toArray()[0];
            std = matStd.toArray()[0];
            Imgproc.threshold(roiImage, roiImage, mean + std, 255, Imgproc.THRESH_BINARY);
            roiImage.copyTo(aux);
        }

        sz = new Size(image.getWidth(), image.getHeight());
        Imgproc.resize(tempImageMat, tempImageMat, sz);
        return tempImageMat;
    }

    public ArrayList<Rect> boundingBox(Mat imgToProcess) {
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<Rect> rects = new ArrayList<>(50);
        Mat hierarchy = new Mat();
//        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf type
        Imgproc.findContours(imgToProcess, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {

//            double contourArea = Imgproc.contourArea(contours.get(contourIdx));

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());

            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);
            Log.d(TAG, "Rect Height :" + rect.height);
            //Log.d(TAG, "Rect Width :" + rect.width);
            if (rect.height > 5) {
                rects.add(rect);
            }
        }
        filterRectangles(rects);
        return rects;
    }

    private ArrayList<Rect> filterRectangles(ArrayList<Rect> rects) {
        double sum = 0.0;
        double mean = 0.0;
        for (int i = 0; i < rects.size(); i++) {
            sum += rects.get(i).height;
        }

        mean = sum / rects.size();
        Log.d(TAG, "Mean = " + mean);

        for (int i = 0; i < rects.size(); i++) {
            if (rects.get(i).height < (mean - 5.0)) {
                rects.remove(i);
            }
        }
        return rects;
    }
}