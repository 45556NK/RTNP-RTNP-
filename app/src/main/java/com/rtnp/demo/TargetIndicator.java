package com.rtnp.demo;

import android.graphics.Color;

public class TargetIndicator {
    public float worldX, worldY;
    public long startTime;
    public int colorR = 255, colorG = 255, colorB = 0;

    public TargetIndicator(float x, float y) {
        worldX = x; worldY = y;
        startTime = System.currentTimeMillis();
    }

    public TargetIndicator(float x, float y, int col) {
        worldX = x; worldY = y;
        colorR = Color.red(col); colorG = Color.green(col); colorB = Color.blue(col);
        startTime = System.currentTimeMillis();
    }
}
