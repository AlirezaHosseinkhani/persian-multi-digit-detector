package com.example.tflite;

import android.graphics.Bitmap;

public class RoiObject implements Comparable<RoiObject> {
    int xCord;
    Bitmap bmp;

    public RoiObject(int xCord, Bitmap bmp) {
        this.xCord = xCord;
        this.bmp = bmp;
    }

    public int compareTo(RoiObject roi) {
        if (this.xCord < roi.xCord) {
            return -1;
        } else if (this.xCord > roi.xCord) {
            return 1;
        } else {
            return 0;
        }
    }
}