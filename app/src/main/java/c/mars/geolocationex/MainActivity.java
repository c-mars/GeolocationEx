package c.mars.geolocationex;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.t)
    TextView t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        Timber.plant(new Timber.DebugTree());
        t.setOnClickListener(v -> t.setText("clicked"));
    }
}
