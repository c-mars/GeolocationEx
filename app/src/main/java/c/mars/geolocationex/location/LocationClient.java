package c.mars.geolocationex.location;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;

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
    @Getter private boolean connected;
    private Location lastLoc;
    private String lastTime;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks=new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            callbacks.connected(connected=true);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };
    private GoogleApiClient.OnConnectionFailedListener failedListener=new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            callbacks.connected(connected=false);
        }
    };

    public LocationClient(Context context, Callbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        client=new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this.connectionCallbacks)
                .addOnConnectionFailedListener(this.failedListener)
                .build();
    }

    public void connect(){
        client.connect();
    }

    public void disconnect(){
        client.disconnect();
    }

    public Location getLocation(){
        return (connected? LocationServices.FusedLocationApi.getLastLocation(client):null);
    }

    public void subscribe(LocationChangesListener locationChangesListener){

        LocationRequest locationRequest= new LocationRequest();
        locationRequest.setInterval(10_000);
        locationRequest.setFastestInterval(5_000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        this.listener = location -> {
            locationChangesListener.update(location);
            this.lastLoc=location;
            this.lastTime=DateFormat.getTimeInstance().format(location.getTime());
        };

        LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this.listener);
    }

    public void unsubscribe(){
        LocationServices.FusedLocationApi.removeLocationUpdates(client, listener);
        listener =null;
    }

    public boolean isSubscribed(){
        return listener !=null;
    }

    public void saveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQ_LOC_UP, listener !=null);
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
                lastTime= savedInstanceState.getString(TIME_K);

                updater.update(lastLoc.toString() + lastTime);
            }

        }
    }

    interface Callbacks{
        void connected(boolean b);
    }

    interface LocationChangesListener{
        void update(Location location);
    }

    interface Updater{
        void update(String m);
    }

    public static class GoefenceIntentService extends IntentService {

        public GoefenceIntentService() {
            super(GoefenceIntentService.class.getName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {

        }
    }
}
