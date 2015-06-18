package c.mars.geolocationex.location;

import com.google.android.gms.maps.model.LatLng;

import lombok.Data;
import lombok.Getter;

/**
 * Created by mars on 6/18/15.
 */
@Data
public class MapManager {
    private static MapManager instance;
    public static MapManager getInstance(){
        if(instance==null){
            instance=new MapManager();
        }
        return instance;
    }

    private int radius;
    private LatLng latLng;

}
