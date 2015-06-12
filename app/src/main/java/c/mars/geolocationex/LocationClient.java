package c.mars.geolocationex;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import lombok.Data;
import lombok.Getter;

/**
 * Created by mars on 6/11/15.
 */
@Data
public class LocationClient {
    final private GoogleApiClient client;
    final private Context context;
    final private Callbacks callbacks;
    @Getter private boolean connected;

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

    interface Callbacks{
        void connected(boolean b);
    }

    interface LocationChangesListener{
        void update(Location location);
    }
    private LocationChangesListener listener;

    private LocationListener locationListener;
    public void subscribe(LocationChangesListener listener){
        this.listener=listener;

        LocationRequest locationRequest= new LocationRequest();
        locationRequest.setInterval(10_000);
        locationRequest.setFastestInterval(5_000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationListener = location -> listener.update(location);

        LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, locationListener);
    }

    public void unsubscribe(){
        LocationServices.FusedLocationApi.removeLocationUpdates(client, locationListener);
        listener=null;
    }

    public boolean isSubscribed(){
        return listener!=null;
    }
}
