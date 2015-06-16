package c.mars.geolocationex.location;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import timber.log.Timber;

/**
 * Created by mars on 6/11/15.
 */
@Data
public class LocationClient implements LocationInterface {
    public static final String TRANSITION="TRANSITION";
    public static final String TA="TA";
    private final static String REQ_LOC_UP = "REQ_LOC_UP";
    private final static String LOC_K = "LOC_K";
    private final static String TIME_K = "TIME_K";
    final private GoogleApiClient client;
    final private Context context;
    final private Callbacks callbacks;
    ResultCallback resultCallback;
    BroadcastReceiver geofencingReceiver;
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
    private Location lastLocation = null;
    private boolean addressRequested = false;
    private AddressResultReceiver receiver;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> geofences = new ArrayList<>();
    private GeofencingCallback geofencingCallback;

    public LocationClient(Context context, Callbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        client = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this.connectionCallbacks)
                .addOnConnectionFailedListener(this.failedListener)
                .build();
        geofencingReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(TA)) {
                    String m = intent.getStringExtra(TRANSITION);
                    geofencingCallback.transition(m);
                }
            }
        };
        context.registerReceiver(geofencingReceiver, new IntentFilter(TA));
    }

    @Override
    public void connect() {
        client.connect();
    }

    @Override
    public void disconnect() {
        client.disconnect();
    }

    @Override
    public Location getLocation() {
        return (connected ? LocationServices.FusedLocationApi.getLastLocation(client) : null);
    }

    @Override
    public void subscribeForLocation(LocationChangesListener locationChangesListener) {

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

    @Override
    public void unsubscribeFromLocation() {
        LocationServices.FusedLocationApi.removeLocationUpdates(client, listener);
        listener = null;
    }

    public boolean isSubscribed() {
        return listener != null;
    }

    @Override
    public void saveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQ_LOC_UP, listener != null);
        savedInstanceState.putParcelable(LOC_K, lastLoc);
        savedInstanceState.putString(TIME_K, lastTime);
    }

    @Override
    public void restoreValuesFromBundle(Bundle savedInstanceState, LocationChangesListener locationChangesListener, Updater updater) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQ_LOC_UP)) {
                subscribeForLocation(locationChangesListener);
            }

            if (savedInstanceState.keySet().contains(LOC_K) && savedInstanceState.keySet().contains(TIME_K)) {
                lastLoc = savedInstanceState.getParcelable(LOC_K);
                lastTime = savedInstanceState.getString(TIME_K);

                updater.update(lastLoc.toString() + lastTime);
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

    @Override
    public void resolveAddress(Location location, AddressCallback addressCallback) {
        this.addressCallback = addressCallback;

        if (!client.isConnected() || location == null) {
            lastLocation = location;
            addressRequested = true;
            return;
        }

        Intent intent = new Intent(context, LocationAddressIntentService.class);
        intent.putExtra(LocationAddressIntentService.REC, receiver);
        intent.putExtra(LocationAddressIntentService.LOC_E, location);
        context.startService(intent);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofencingIntentService.class);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void geofencingStart(GeofencingCallback geofencingCallback) {
        this.geofencingCallback=geofencingCallback;
        resultCallback= result -> {
            Timber.d(result.toString());
            geofencingCallback.transition("result: "+result.toString());
        };
        LocationServices.GeofencingApi.addGeofences( client, getGeofencingRequest(), getGeofencePendingIntent()).setResultCallback(resultCallback);
    }

    @Override
    public void geofencingStop() {
        LocationServices.GeofencingApi.removeGeofences(client, getGeofencePendingIntent()).setResultCallback(resultCallback);
    }

    @Override
    public void addGeofence(String idString, Location location, float radiusMeters, long expiresInMs) {

        geofences.add(new Geofence.Builder()
                .setRequestId(idString)
                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        radiusMeters
                )
                .setExpirationDuration(expiresInMs)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    public interface Callbacks {
        void connected(boolean b);
    }

    private class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String m = resultData.getString(LocationAddressIntentService.RES_K);
            addressCallback.onAddress(m);
            if (resultCode == LocationAddressIntentService.SUCC) {
                Toast.makeText(context, "Resolved", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
