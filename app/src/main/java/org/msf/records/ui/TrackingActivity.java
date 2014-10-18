package org.msf.records.ui;

import com.estimote.sdk.Region;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Utils;

import java.util.List;


/**
 * Created by Gil on 01/10/2014.
 */
public class TrackingActivity extends FragmentActivity implements BeaconManager.RangingListener {

    private static final String TAG = TrackingActivity.class.getSimpleName();

    private NfcAdapter mNFCAdapter;
    private IntentFilter intentFiltersArray[];
    private PendingIntent intent;
    private String techListsArray[][];


    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);

    private BeaconManager beaconManager = new BeaconManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beaconManager.setRangingListener(this);

        mNFCAdapter = NfcAdapter.getDefaultAdapter(this);
        intent = PendingIntent.getActivity(this, 0, new Intent(this,
                TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Unable to speciy */* Mime Type", e);
        }
        intentFiltersArray = new IntentFilter[] { ndef };

        techListsArray = new String[][] { new String[] { NfcV.class.getName() } };

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	    /*StringBuilder sb = new StringBuilder();
	    for(int i = 0; i < tag.getId().length; i++){
	    	sb.append(new Integer(tag.getId()[i]) + " ");
	    }*/
        Toast.makeText(this, "TagID: " +
                org.msf.records.utils.Utils.bytesToHex(tag.getId()), Toast.LENGTH_SHORT);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mNFCAdapter.enableForegroundDispatch(this, intent, intentFiltersArray, techListsArray);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot stop but it does not matter now", e);
        }
        mNFCAdapter.disableForegroundDispatch(this);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.disconnect();
    }

    @Override
    public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
        //Remember this isn't called in the UI thread
        for (Beacon beacon : beacons){
            double distance = Utils.computeAccuracy(beacon);
            if (distance >= 0 && distance <= 0.5){
                Toast.makeText(this, "Beacon in range: " + distance + "m ", Toast.LENGTH_SHORT);
            }
        }
    }
}
