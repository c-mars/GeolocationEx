package c.mars.geolocationex.location;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
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
import timber.log.Timber;

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
    private AddressCallback addressCallback;
    @Getter
    private boolean connected;
    private Location lastLoc;
    private String lastTime;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            callbacks.connected(connected = true);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };
    private GoogleApiClient.OnConnectionFailedListener failedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            callbacks.connected(connected = false);
        }
    };
    private static final String TRANSITION="TRANSITION";
    private static final String TA="TA";
    public LocationClient(Context context, Callbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        client = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this.connectionCallbacks)
                .addOnConnectionFailedListener(this.failedListener)
                .build();
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(TA)) {
                    String m = intent.getStringExtra(TRANSITION);
                    geofencingCallback.transition(m);
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(TA));
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
            this.lastLoc = location;
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
        savedInstanceState.putParcelable(LOC_K, lastLoc);
        savedInstanceState.putString(TIME_K, lastTime);
    }

    public void restoreValuesFromBundle(Bundle savedInstanceState, LocationChangesListener locationChangesListener, Updater updater) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQ_LOC_UP)) {
                subscribe(locationChangesListener);
            }

            if (savedInstanceState.keySet().contains(LOC_K) && savedInstanceState.keySet().contains(TIME_K)) {
                lastLoc = savedInstanceState.getParcelable(LOC_K);
                lastTime = savedInstanceState.getString(TIME_K);

                updater.update(lastLoc.toString() + lastTime);
            }

        }
    }

    private Location lastLocation = null;
    private boolean addressRequested = false;
    private AddressResultReceiver receiver;
    public void resolveAddress(Location location, AddressCallback addressCallback) {
        this.addressCallback = addressCallback;

        if (!client.isConnected() || location == null) {
            lastLocation = location;
            addressRequested = true;
            return;
        }

        Intent intent = new Intent(context, LocationIntentService.class);
        intent.putExtra(LocationIntentService.REC, receiver);
        intent.putExtra(LocationClient.LocationIntentService.LOC_E, location);
        context.startService(intent);
    }

    public interface Callbacks {
        void connected(boolean b);
    }

    public interface LocationChangesListener {
        void update(Location location);
    }

    public interface Updater {
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

    public static class GeofencingIntentService extends IntentService {

        public GeofencingIntentService() {
            super(GeofencingIntentService.class.getName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {

            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
//                geofencingEvent.getErrorCode();
//                String errorMessage = GeofenceErrorMessages.getErrorString(this,
//                        geofencingEvent.getErrorCode());
                return;
            }

            Integer geofenceTransition = geofencingEvent.getGeofenceTransition();

            Intent i=new Intent(TA);
            i.putExtra(TRANSITION, geofenceTransition.toString());
            sendBroadcast(i);

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            List triggeringGeofences = geofencingEvent.getTriggeringGeofences();

                Timber.d(triggeringGeofences.toString());
                // Get the transition details as a String.
//                String geofenceTransitionDetails = getGeofenceTransitionDetails(
//                        this,
//                        geofenceTransition,
//                        triggeringGeofences
//                );

                // Send notification and log the transition details.
//                sendNotification(geofenceTransitionDetails);

            } else {
                // Log the error.

            }
        }
    }




//    private class Entry {
//        public String id;
//        public Location location;
//        public float radius;
//
//        public Entry(String id, Location location, float radius) {
//            this.id = id;
//            this.location = location;
//            this.radius = radius;
//        }
//    }



    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent geofencePendingIntent;

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofencingIntentService.class);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    ResultCallback resultCallback;


    public void geofencingStart(GeofencingCallback geofencingCallback) {
        this.geofencingCallback=geofencingCallback;
        resultCallback= result -> {
            Timber.d(result.toString());
            geofencingCallback.transition("add geo res: "+result.toString());
        };
        LocationServices.GeofencingApi.addGeofences( client, getGeofencingRequest(), getGeofencePendingIntent()).setResultCallback(resultCallback);
    }

    public void geofencingStop() {
        LocationServices.GeofencingApi.removeGeofences(client, getGeofencePendingIntent()).setResultCallback(resultCallback);
    }

    private List<Geofence> geofences = new ArrayList<>();

    public void addGeofence(String id, Location location, float r, long expiration) {

        geofences.add(new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        r
                )
                .setExpirationDuration(expiration)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    public interface GeofencingCallback{
        void transition(String m);
    }
    private GeofencingCallback geofencingCallback;
    BroadcastReceiver receiver;
}
