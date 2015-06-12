package c.mars.geolocationex;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
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
    @InjectView(R.id.gf)
    Button gf;
    View[] vs;
    private boolean b;
    private LocationClient locationClient;

    @OnClick(R.id.cl)
    void cl() {
        Location l = locationClient.getLocation();
        if (l != null) {
            t.setText(l.toString());
        }
    }

    @OnClick(R.id.lu)
    void lu() {

    }

    @OnClick(R.id.gf)
    void gf() {

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
                }
            } else {
                String s = "connected: " + connected;
                Timber.d(s);
                t.setText(s);
            }
        });

        draw(i);
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
}
