package c.mars.geolocationex;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import c.mars.geolocationex.location.LocationClient;
import c.mars.geolocationex.location.LocationInterface;
import c.mars.geolocationex.location.MapsActivity;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.t)
    TextView t;
    @InjectView(R.id.i)
    ImageView i;
    @InjectView(R.id.cl)
    Button cl;
    @InjectView(R.id.lu)
    Button lu;

    MapFragment map;

    @OnClick(R.id.m)
            void map(){
        startActivity(new Intent(this, MapsActivity.class));
    }

    View[] vs;
    @InjectView(R.id.gf)
    Button gf;
    private LocationInterface locationClient;
    private Boolean marker = false;
    private boolean gfOn=false;

    @OnClick(R.id.cl)
    void cl() {
        Location l = locationClient.getLocation();
        locationClient.resolveAddress(l, address -> t.append("\n - "+address));
        if (l != null) {
            t.setText(l.toString());
        } else {
            Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.lu)
    void lu() {
        if (!locationClient.isSubscribed()) {
            locationClient.subscribeForLocation(location -> showLocation());
            lu.setText("unsubscribe");
        } else {
            locationClient.unsubscribeFromLocation();
            t.setText("unsubscribed");
            Toast.makeText(this, "unsubscribed", Toast.LENGTH_LONG).show();
            lu.setText("subscribe");
        }
    }

    @OnClick(R.id.gf)
    void gf() {

//        todo display map to select geofence manually, change radius
        if(gfOn=!gfOn) {
            Location location=locationClient.getLocation();
            locationClient.addGeofence("geo", location, 5, 10*60*1000); // 5 meters, 10 minutes
            locationClient.geofencingStart(m -> t.setText("geo transition: "+m));
            gf.setText("geo on");
        }else {
            locationClient.geofencingStop();
            gf.setText("geo off");
        }
    }

    private void enable() {
        for (View v : vs)
            v.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        Timber.plant(new Timber.DebugTree());

        vs = new View[]{cl, lu, gf};

        locationClient = new LocationClient(this, connected -> {
            if (connected) {
                enable();
                Location l = locationClient.getLocation();
                if (l != null) {
                    t.setText(l.toString());
                    locationClient.resolveAddress(l, address -> t.append(" - " + address));
                }
                setMap();
            } else {
                String s = "connected: " + connected;
                Timber.d(s);
                t.setText(s);
            }
        });

        draw(i);

        if (savedInstanceState != null) {
            locationClient.restoreValuesFromBundle(savedInstanceState, location -> t.setText(location.toString()), m -> t.append(" last: " + m));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setMap(){
        Location location=locationClient.getLocation();
        if(location==null){
            return;
        }

        map = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        if(map!=null) {
//            map.getMap().setMyLocationEnabled(true);
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());

            map.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 14));
            CircleOptions circleOptions=new CircleOptions();

            circleOptions.center(ll);
            circleOptions.radius(100);
            circleOptions.fillColor(Color.argb(125, 0, 255, 0));
            circleOptions.strokeColor(Color.GREEN);
            map.getMap().addCircle(circleOptions);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        locationClient.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        locationClient.disconnect();
        super.onStop();
    }

    private void draw(@NonNull ImageView v) {
        try {
            SVG svg = SVG.getFromResource(this, R.raw.location);
            Picture picture = svg.renderToPicture();
            Bitmap bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawPicture(picture);
            v.setImageBitmap(bitmap);
        } catch (SVGParseException e) {
            e.printStackTrace();
        }
    }

    private void showLocation(){
        Location location=locationClient.getLocation();
        locationClient.resolveAddress(location, address -> t.setText(location.toString() + " - " + address));
    }
}
