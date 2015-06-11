package c.mars.geolocationex;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import lombok.Data;

/**
 * Created by mars on 6/11/15.
 */
@Data
public class LocationClient {
    final private GoogleApiClient client;
    final private Context context;
    final private Callbacks callbacks;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks=new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            callbacks.connected(true);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };
    private GoogleApiClient.OnConnectionFailedListener failedListener=new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            callbacks.connected(false);
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
        return LocationServices.FusedLocationApi.getLastLocation(client);
    }

    interface Callbacks{
        void connected(boolean b);
    }
}
