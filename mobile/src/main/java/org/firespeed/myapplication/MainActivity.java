package org.firespeed.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.firespeed.both.Config;

public class MainActivity extends AppCompatActivity {
    private Config mConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final CheckBox smooth = (CheckBox)findViewById(R.id.smooth);
        mConfig = new Config(this, new Config.OnConfigChangedListener() {
            @Override
            public void onConfigChanged(Config config) {
                Log.d("Config", String.valueOf(config.isSmooth()));
                if(mConfig.isSmooth() != smooth.isChecked()) {
                    smooth.setChecked(config.isSmooth());
                }
            }
        });
        smooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mConfig.isSmooth() != isChecked) {
                    mConfig.setIsSmooth(isChecked);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConfig.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConfig.disconnect();
    }
}
