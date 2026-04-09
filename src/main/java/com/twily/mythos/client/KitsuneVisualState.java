package com.twily.mythos.client;

public final class KitsuneVisualState {

    private static boolean tailHidden;

    private KitsuneVisualState() {
    }

    public static boolean tailHidden() {
        return tailHidden;
    }

    public static void toggleTailHidden() {
        tailHidden = !tailHidden;
    }
}
