package c.mars.geolocationex.location;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by mars on 6/16/15.
 */
public class LocationAddressIntentService extends IntentService {

    public static final int SUCC = 0;
    public static final int FAIL = 1;
    public static final String PKG = LocationAddressIntentService.class.getPackage().getName();
    public static final String REC = PKG + ".REC";
    public static final String RES_K = PKG + ".RES_K";
    public static final String LOC_E = PKG + ".LOC_E";
    private ResultReceiver receiver;

    public LocationAddressIntentService() {
        super(LocationAddressIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String m = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        Location location = intent.getParcelableExtra(LOC_E);
        receiver = intent.getParcelableExtra(REC);

        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            m = "Service not available";
        } catch (IllegalArgumentException ia) {
            m = "Invalid location: [lat=" + location.getLatitude() + ", long=" + location.getLongitude() + "]";
        }

        if (addresses == null || addresses.isEmpty()) {
            if (m.isEmpty()) {
                m = "No addresses found";
            }
            deliverResultToReceiver(FAIL, m);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> aFrags = new ArrayList<>();
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                aFrags.add(address.getAddressLine(i));
            }
            deliverResultToReceiver(SUCC, TextUtils.join(System.getProperty("line.separator"), aFrags));
        }

    }

    private void deliverResultToReceiver(int rCode, String m) {
        Bundle bundle = new Bundle();
        bundle.putString(RES_K, m);
        receiver.send(rCode, bundle);
    }

}
