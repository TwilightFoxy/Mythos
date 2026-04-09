package com.twily.mythos.client;

import java.util.Locale;

public final class KitsuneTailDebugState {

    private static float offsetX;
    private static float offsetY;
    private static float offsetZ;
    private static float rotX;
    private static float rotY;
    private static float rotZ;
    private static float step = 0.05F;

    private KitsuneTailDebugState() {
    }

    public static float offsetX() {
        return offsetX;
    }

    public static float offsetY() {
        return offsetY;
    }

    public static float offsetZ() {
        return offsetZ;
    }

    public static float step() {
        return step;
    }

    public static float rotX() {
        return rotX;
    }

    public static float rotY() {
        return rotY;
    }

    public static float rotZ() {
        return rotZ;
    }

    public static void nudgeX(float delta) {
        offsetX += delta;
    }

    public static void nudgeY(float delta) {
        offsetY += delta;
    }

    public static void nudgeZ(float delta) {
        offsetZ += delta;
    }

    public static void nudgeRotX(float delta) {
        rotX += delta;
    }

    public static void nudgeRotY(float delta) {
        rotY += delta;
    }

    public static void nudgeRotZ(float delta) {
        rotZ += delta;
    }

    public static void increaseStep() {
        step = Math.min(0.25F, step + 0.01F);
    }

    public static void decreaseStep() {
        step = Math.max(0.005F, step - 0.01F);
    }

    public static void reset() {
        offsetX = 0.0F;
        offsetY = 0.0F;
        offsetZ = 0.0F;
        rotX = 0.0F;
        rotY = 0.0F;
        rotZ = 0.0F;
        step = 0.05F;
    }

    public static String summary() {
        return String.format(Locale.ROOT, "x=%.3f y=%.3f z=%.3f rx=%.1f ry=%.1f rz=%.1f step=%.3f", offsetX, offsetY, offsetZ, rotX, rotY, rotZ, step);
    }
}
