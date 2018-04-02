package com.example.ian.mycompass;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    private static final String TAG = "AddressIntentService";

    protected ResultReceiver resultReceiver;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
//        Log.d(TAG, "onHandleIntent");
        if (intent == null) {
            return;
        }

        String errorMessage = "";
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(this, Locale.TAIWAN);
        Location location = intent.getParcelableExtra(GeoConstants.LOCATION_DATA_EXTRA);
        resultReceiver = intent.getParcelableExtra(GeoConstants.RECEIVER);

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );
        } catch (IOException e) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, e);
        } catch (IllegalArgumentException e) {
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage, e);
        }

        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(GeoConstants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            deliverResultToReceiver(
                    GeoConstants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments)
            );
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(GeoConstants.RESULT_DATA_KEY, message);
        resultReceiver.send(resultCode, bundle);
    }
}
