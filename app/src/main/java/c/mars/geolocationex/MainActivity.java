package c.mars.geolocationex;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.t)
    TextView t;
    private boolean b;
    private LocationClient locationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        Timber.plant(new Timber.DebugTree());

        t.setOnClickListener(v -> t.setText("clicked: "+Boolean.toString(b=!b)));
        locationClient=new LocationClient(this, b1 -> {
            String s="connected: "+ b1;
            Timber.d(s);
            t.setText(s);
        });
        locationClient.connect();
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
}
