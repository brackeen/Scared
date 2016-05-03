package com.brackeen.scared;

import java.util.prefs.Preferences;

public class Settings {

    public static final String AUTO_PIXEL_SCALE = "autoPixelScale";
    public static final String DEPTH_SHADING = "depthShading";
    public static final String VOLUME = "volume";

    public static Preferences getPrefs() {
        Preferences prefs;
        try {
            prefs = Preferences.userNodeForPackage(Settings.class);
        } catch (SecurityException ex) {
            prefs = null;
        }
        return prefs;
    }

    public static void putFloat(String name, float value) {
        Preferences prefs = getPrefs();
        if (prefs != null) {
            prefs.putFloat(name, value);
        }
    }

    public static float getFloat(String name, float defaultValue) {
        Preferences prefs = getPrefs();
        if (prefs != null) {
            return prefs.getFloat(name, defaultValue);
        } else {
            return defaultValue;
        }
    }

    public static void putBoolean(String name, boolean value) {
        Preferences prefs = getPrefs();
        if (prefs != null) {
            prefs.putBoolean(name, value);
        }
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        Preferences prefs = getPrefs();
        if (prefs != null) {
            return prefs.getBoolean(name, defaultValue);
        } else {
            return defaultValue;
        }
    }
}
