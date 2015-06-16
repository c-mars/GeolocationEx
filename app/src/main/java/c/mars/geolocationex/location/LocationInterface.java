package c.mars.geolocationex.location;

import android.location.Location;
import android.os.Bundle;

/**
 * Created by mars on 6/15/15.
 */
public interface LocationInterface {
//    init
    void connect();
    void disconnect();
    void saveInstanceState(Bundle savedInstanceState);
    void restoreValuesFromBundle(Bundle savedInstanceState, LocationChangesListener locationChangesListener, Updater updater);

//    instant location
    Location getLocation();

    void resolveAddress(Location location, AddressCallback addressCallback);

//    location changes
    void subscribeForLocation(LocationChangesListener locationChangesListener);

    void unsubscribeFromLocation();

    boolean isSubscribed();

//    geofencing
    void addGeofence(String idString, Location location, float radiusMeters, long expiresInMs);

    void geofencingStop();

    void geofencingStart(GeofencingCallback geofencingCallback);

    interface Updater {
        void update(String m);
    }
    interface AddressCallback {
        void onAddress(String address);
    }
    interface LocationChangesListener {
        void update(Location location);
    }
    interface GeofencingCallback{
        void transition(String m);
    }
}
