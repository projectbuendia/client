package org.msf.records.controllers;

import android.content.Intent;

import org.msf.records.ui.ControllableActivity;

/**
 * Created by dxchen on 12/3/14.
 */
public interface ActivityController {

    void onActivityResult(
            ControllableActivity activity, int requestCode, int resultCode, Intent data);
}
