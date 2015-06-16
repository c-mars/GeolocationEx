package c.mars.geolocationex.location;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import timber.log.Timber;

/**
 * Created by mars on 6/16/15.
 */
public class GeofencingIntentService extends IntentService {

    public GeofencingIntentService() {
        super(GeofencingIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Intent i = new Intent(LocationClient.TA);
            i.putExtra(LocationClient.TRANSITION, "error: "+geofencingEvent.getErrorCode());
            sendBroadcast(i);
//                geofencingEvent.getErrorCode();
//                String errorMessage = GeofenceErrorMessages.getErrorString(this,
//                        geofencingEvent.getErrorCode());
            return;
        }

        Integer geofenceTransition = geofencingEvent.getGeofenceTransition();

        Intent i = new Intent(LocationClient.TA);
        i.putExtra(LocationClient.TRANSITION, geofenceTransition.toString());
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
