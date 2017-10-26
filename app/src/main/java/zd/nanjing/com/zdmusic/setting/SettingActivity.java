package zd.nanjing.com.zdmusic.setting;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import zd.nanjing.com.zdmusic.Constant;
import zd.nanjing.com.zdmusic.MusicManager;
import zd.nanjing.com.zdmusic.MusicService;
import zd.nanjing.com.zdmusic.R;
import zd.nanjing.com.zdmusic.main.MainActivity;

/**
 * Created by Administrator on 2017/9/19.
 */

public class SettingActivity extends AppCompatActivity {


    CheckBox cb_remember_last;
    CheckBox cb_scan_below_30;
    private MusicManager musicManager;
    private ServiceConnection serviceConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        cb_remember_last = (CheckBox) findViewById(R.id.cb_remember_last);
        cb_scan_below_30 = (CheckBox) findViewById(R.id.cb_scan_music_below_30);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {


                musicManager = MusicManager.Stub.asInterface(iBinder);
                if (musicManager != null) {
                    try {
                        cb_remember_last.setChecked(musicManager.isRememberLastMusic());
                        cb_scan_below_30.setChecked(musicManager.isScanShortMusic());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }


            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };


        Intent mIntent = new Intent(SettingActivity.this, MusicService.class);

        bindService(mIntent, serviceConnection, BIND_AUTO_CREATE);


        cb_scan_below_30.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                try {
                    musicManager.setScanShortMusic(cb_scan_below_30.isChecked());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });


        cb_remember_last.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                try {
                    musicManager.setRememberLastMusic(cb_remember_last.isChecked());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}
