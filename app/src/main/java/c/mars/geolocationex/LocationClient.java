package c.mars.geolocationex;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.Data;
import lombok.Getter;

/**
 * Created by mars on 6/11/15.
 */
@Data
public class LocationClient {
    private final static String REQ_LOC_UP = "REQ_LOC_UP";
    private final static String LOC_K = "LOC_K";
    private final static String TIME_K = "TIME_K";
    final private GoogleApiClient client;
    final private Context context;
    final private Callbacks callbacks;
    private LocationListener listener;
    @Getter
    private boolean connected;
    private String lastTime;
    private GoogleApiClient.OnConnectionFailedListener failedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            callbacks.connected(connected = false);
        }
    };
    private AddressResultReceiver receiver;
    private boolean addressRequested = false;
    private Location lastLocation = null;
    private AddressCallback addressCallback;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            callbacks.connected(connected = true);
            if (addressRequested) {
                if (!Geocoder.isPresent()) {
                    Toast.makeText(context, "No geocoder available", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (addressCallback != null) {
                    resolveAddress(lastLocation, addressCallback);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    public LocationClient(Context context, Callbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        this.receiver=new AddressResultReceiver(new Handler(Looper.getMainLooper()));

        client = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this.connectionCallbacks)
                .addOnConnectionFailedListener(this.failedListener)
                .build();
    }

    public void connect() {
        client.connect();
    }

    public void disconnect() {
        client.disconnect();
    }

    public Location getLocation() {
        return (connected ? LocationServices.FusedLocationApi.getLastLocation(client) : null);
    }

    public void subscribe(LocationChangesListener locationChangesListener) {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10_000);
        locationRequest.setFastestInterval(5_000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        this.listener = location -> {
            locationChangesListener.update(location);
            this.lastLocation = location;
            this.lastTime = DateFormat.getTimeInstance().format(location.getTime());
        };

        LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this.listener);
    }

    public void unsubscribe() {
        LocationServices.FusedLocationApi.removeLocationUpdates(client, listener);
        listener = null;
    }

    public boolean isSubscribed() {
        return listener != null;
    }

    public void saveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQ_LOC_UP, listener != null);
        savedInstanceState.putParcelable(LOC_K, lastLocation);
        savedInstanceState.putString(TIME_K, lastTime);
    }

    public void restoreValuesFromBundle(Bundle savedInstanceState, LocationChangesListener locationChangesListener, Updater updater) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQ_LOC_UP)) {
                subscribe(locationChangesListener);
            }

            if (savedInstanceState.keySet().contains(LOC_K) && savedInstanceState.keySet().contains(TIME_K)) {
                lastLocation = savedInstanceState.getParcelable(LOC_K);
                lastTime = savedInstanceState.getString(TIME_K);

                updater.update(lastLocation.toString() + lastTime);
            }
        }
    }

    public void resolveAddress(Location location, AddressCallback addressCallback) {
        this.addressCallback = addressCallback;

        if (!client.isConnected() || location == null) {
            lastLocation = location;
            addressRequested = true;
            return;
        }

        Intent intent = new Intent(context, LocationIntentService.class);
        intent.putExtra(LocationIntentService.REC, receiver);
        intent.putExtra(LocationIntentService.LOC_E, location);
        context.startService(intent);
    }

    interface Callbacks {
        void connected(boolean b);
    }

    interface LocationChangesListener {
        void update(Location location);
    }

    interface Updater {
        void update(String m);
    }

    public interface AddressCallback {
        void onAddress(String address);
    }

    public static class LocationIntentService extends IntentService {

        public static final int SUCC = 0;
        public static final int FAIL = 1;
        public static final String PKG = LocationIntentService.class.getPackage().getName();
        public static final String REC = PKG + ".REC";
        public static final String RES_K = PKG + ".RES_K";
        public static final String LOC_E = PKG + ".LOC_E";
        private ResultReceiver receiver;

        public LocationIntentService() {
            super(LocationIntentService.class.getName());
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

    private class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String m = resultData.getString(LocationIntentService.RES_K);
            addressCallback.onAddress(m);
            if (resultCode == LocationIntentService.SUCC) {
                Toast.makeText(context, "Resolved", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
