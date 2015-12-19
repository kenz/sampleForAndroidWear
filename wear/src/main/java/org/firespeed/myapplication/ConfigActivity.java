package org.firespeed.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.firespeed.both.Config;

public class ConfigActivity extends Activity {

    private Config mConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        final CheckBox smooth = (CheckBox)findViewById(R.id.smooth);
        mConfig = new Config(this, new Config.OnConfigChangedListener() {
            @Override
            public void onConfigChanged(Config config) {
                smooth.setChecked(config.isSmooth());
            }
        });
        smooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mConfig.setIsSmooth(isChecked);
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
