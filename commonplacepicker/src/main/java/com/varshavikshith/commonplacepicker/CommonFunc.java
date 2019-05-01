package com.varshavikshith.commonplacepicker;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class CommonFunc {

    public static boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    public static boolean isGpsOn(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean status = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return status;
    }

    public static String returnDecimals(double doubleValue, int length) {
        String formate = "%." + length + "f";
        return String.format(formate, doubleValue);
    }

}
