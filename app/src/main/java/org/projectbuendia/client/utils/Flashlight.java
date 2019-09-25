package org.projectbuendia.client.utils;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import org.projectbuendia.client.App;

public class Flashlight {
    private static Flashlight instance = null;

    private CameraManager cam = null;
    private String cameraId = null;
    private boolean active = false;

    public static Flashlight get() {
        if (instance == null) {
            instance = new Flashlight(App.getContext());
        }
        return instance;
    }

    private Flashlight(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            cam = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String id : cam.getCameraIdList()) {
                    Boolean hasFlash = cam.getCameraCharacteristics(id).get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (hasFlash != null && hasFlash) {
                        cameraId = id;
                    }
                }
            } catch (CameraAccessException e) { }
            activate(false);
        }
    }

    public boolean isAvailable() {
        return cam != null && cameraId != null;
    }

    public boolean isActive() {
        return active;
    }

    public void activate(boolean active) {
        if (isAvailable() && Build.VERSION.SDK_INT >= 23) {
            try {
                cam.setTorchMode(cameraId, active);
            } catch (CameraAccessException e) { }
        }
        this.active = active;
    }

    public void toggle() {
        activate(!active);
    }
}
