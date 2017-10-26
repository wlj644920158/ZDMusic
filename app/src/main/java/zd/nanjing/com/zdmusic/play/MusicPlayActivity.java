package zd.nanjing.com.zdmusic.play;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import zd.nanjing.com.zdmusic.AlbumImageLoader;
import zd.nanjing.com.zdmusic.Constant;
import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicCallback;
import zd.nanjing.com.zdmusic.MusicManager;
import zd.nanjing.com.zdmusic.MusicService;
import zd.nanjing.com.zdmusic.MusicUtil;
import zd.nanjing.com.zdmusic.R;

/**
 * Created by wanglijun on 2017/9/30.
 */

public class MusicPlayActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private ImageView bg;
    private ImageView album;
    private ImageView control_toggle_mode;
    private ImageView control_heart;
    private ImageView control_share;
    private TextView curDuration;
    private TextView duration;
    private SeekBar seekBar;
    private ImageView control_last;
    private ImageView control_next;
    private ImageView control_togglePlay;
    private Toolbar toolbar;
    private Music music;


    private long curProgress;
    private int mode = Constant.MUSIC_MODE_ORDER;
    private int serverStatus;


    private Handler handler;

    private MusicCallback musicCallback;
    private MusicManager musicManager;
    private ServiceConnection serviceConnection;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        album = (ImageView) findViewById(R.id.iv_album);
        bg = (ImageView) findViewById(R.id.iv_bg);
        control_toggle_mode = (ImageView) findViewById(R.id.iv_control_toggle_mode);
        control_heart = (ImageView) findViewById(R.id.iv_control_heart);
        control_share = (ImageView) findViewById(R.id.iv_control_share);

        curDuration = (TextView) findViewById(R.id.tv_curDuration);
        duration = (TextView) findViewById(R.id.tv_duration);

        seekBar = (SeekBar) findViewById(R.id.progress);
        control_last = (ImageView) findViewById(R.id.iv_control_last);
        control_togglePlay = (ImageView) findViewById(R.id.iv_control_toggle_play);
        control_next = (ImageView) findViewById(R.id.iv_control_next);


        control_toggle_mode.setOnClickListener(this);
        control_heart.setOnClickListener(this);
        control_share.setOnClickListener(this);
        control_last.setOnClickListener(this);
        control_togglePlay.setOnClickListener(this);
        control_next.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(this);


        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);
        }


        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constant.MSG_ON_NEW_MUSIC:

                        bg.setImageBitmap(MusicUtil.blurBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()), MusicPlayActivity.this));
                        album.setImageBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()));
                        toolbar.setTitle(music.getMusic_title());
                        control_togglePlay.setImageResource(R.drawable.ic_play_stop_music);
                        curProgress = 0;
                        curDuration.setText("00:00");
                        duration.setText(MusicUtil.duration2String(music.getMusic_duration()));
                        serverStatus = Constant.SERVER_MODE_PLAYING;
                        sendEmptyMessage(Constant.MSG_UPDATE_TIME);
                        break;
                    case Constant.MSG_ON_PLAY:
                        serverStatus = Constant.SERVER_MODE_PLAYING;
                        control_togglePlay.setImageResource(R.drawable.ic_play_stop_music);
                        break;
                    case Constant.MSG_ON_PAUSE:
                        serverStatus = Constant.SERVER_MODE_PAUSE;
                        control_togglePlay.setImageResource(R.drawable.ic_play_play_music);
                        break;
                    case Constant.MSG_TOGGLE_MODE:
                        switch (mode) {
                            case Constant.MUSIC_MODE_ORDER:
                                control_toggle_mode.setImageResource(R.drawable.ic_play_mode_order);
                                break;
                            case Constant.MUSIC_MODE_RANDOM:
                                control_toggle_mode.setImageResource(R.drawable.ic_play_mode_random);
                                break;
                            case Constant.MUSIC_MODE_SINGLE:
                                control_toggle_mode.setImageResource(R.drawable.ic_play_mode_single);
                                break;
                        }
                        break;
                    case Constant.MSG_FINISH_SCAN:
                        break;
                    case Constant.MSG_CONNECT_SUCCESS:
                        if (music.getId() == Constant.LOCAL_MUSIC_DEFAULT_ID) {
                            return;
                        }
                        bg.setImageBitmap(MusicUtil.blurBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()), MusicPlayActivity.this));
                        album.setImageBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()));
                        toolbar.setTitle(music.getMusic_title());
                        control_togglePlay.setImageResource(R.drawable.ic_play_stop_music);
                        curDuration.setText(MusicUtil.duration2String(curProgress));
                        seekBar.setProgress((int) (curProgress / music.getMusic_duration() * 1.f));
                        duration.setText(MusicUtil.duration2String(music.getMusic_duration()));
                        switch (mode) {
                            case Constant.MUSIC_MODE_ORDER:
                                control_toggle_mode.setImageResource(R.drawable.ic_play_mode_order);
                                break;
                            case Constant.MUSIC_MODE_RANDOM:
                                control_toggle_mode.setImageResource(R.drawable.ic_play_mode_random);
                                break;
                            case Constant.MUSIC_MODE_SINGLE:
                                control_toggle_mode.setImageResource(R.drawable.ic_play_mode_single);
                                break;
                        }
                        if (serverStatus == Constant.SERVER_MODE_PLAYING) {
                            control_togglePlay.setImageResource(R.drawable.ic_play_stop_music);
                        } else {
                            control_togglePlay.setImageResource(R.drawable.ic_play_play_music);
                        }
                        if (serverStatus == Constant.SERVER_MODE_PLAYING || serverStatus == Constant.SERVER_MODE_PAUSE)
                            sendEmptyMessage(Constant.MSG_UPDATE_TIME);
                        break;
                    case Constant.MSG_UPDATE_TIME:
                        try {
                            if (serverStatus == Constant.SERVER_MODE_PLAYING) {
                                curProgress = musicManager.curDuration();
                                seekBar.setProgress((int) (curProgress * 1.0f / music.getMusic_duration() * 100));
                                curDuration.setText(MusicUtil.duration2String(curProgress));
                                postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (serverStatus == Constant.SERVER_MODE_PLAYING) {
                                            sendEmptyMessage(Constant.MSG_UPDATE_TIME);
                                        }
                                    }
                                }, 1000);
                            }

                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                        break;
                    case Constant.MSG_MUSIC_LIKE:
                        if ((boolean) msg.obj) {
                            Toast.makeText(MusicPlayActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MusicPlayActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };

        musicCallback = new MusicCallback.Stub() {


            @Override
            public void onFinishScan() throws RemoteException {

            }

            @Override
            public void onNewMusic(Music m) throws RemoteException {
                music = m;
                handler.sendEmptyMessage(Constant.MSG_ON_NEW_MUSIC);
            }

            @Override
            public void onPause() throws RemoteException {
                handler.sendEmptyMessage(Constant.MSG_ON_PAUSE);
            }

            @Override
            public void onPlay() throws RemoteException {
                handler.sendEmptyMessage(Constant.MSG_ON_PLAY);
            }


            @Override
            public void onNewMode(int m) throws RemoteException {
                mode = m;
                handler.sendEmptyMessage(Constant.MSG_TOGGLE_MODE);
            }


            @Override
            public void onLikeMusic(boolean success) throws RemoteException {
                Message message = new Message();
                message.what = Constant.MSG_MUSIC_LIKE;
                message.obj = success;
                handler.sendMessage(message);
            }

            @Override
            public void onMusicError(int errorCode) throws RemoteException {

            }

            @Override
            public void onAddMusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onRemoveMusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onAddMusic2MusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onRemoveMusicFromMusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onDirectoriesChanged() throws RemoteException {

            }


        };


        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        musicManager = MusicManager.Stub.asInterface(iBinder);
                        if (musicManager != null) {
                            try {
                                musicManager.registerCallback(musicCallback);
                                music = musicManager.curMusic();
                                curProgress = musicManager.curDuration();//会触发播放器自动播放
                                mode = musicManager.getMode();
                                serverStatus = musicManager.curStatus();
                                handler.sendEmptyMessage(Constant.MSG_CONNECT_SUCCESS);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        Intent mIntent = new Intent(MusicPlayActivity.this, MusicService.class);
        bindService(mIntent, serviceConnection, BIND_AUTO_CREATE);

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
        serverStatus = Constant.SERVER_MODE_IDL;
        handler.removeMessages(Constant.MSG_UPDATE_TIME);
        try {
            musicManager.unregisterCallback(musicCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(serviceConnection);
    }

    @Override
    public void onClick(final View v) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (v.getId()) {
                        case R.id.iv_control_toggle_mode:
                            musicManager.toggleMode();
                            break;
                        case R.id.iv_control_heart:
                            musicManager.likeMusic(music);
                            break;
                        case R.id.iv_control_share:

                            break;
                        case R.id.iv_control_last:
                            handler.removeMessages(Constant.MSG_UPDATE_TIME);
                            musicManager.lastMusic();
                            break;
                        case R.id.iv_control_toggle_play:
                            musicManager.togglePlay();
                            break;
                        case R.id.iv_control_next:
                            handler.removeMessages(Constant.MSG_UPDATE_TIME);
                            musicManager.nextMusic();
                            break;
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int percent = seekBar.getProgress();
        long toDuration = (long) (music.getMusic_duration() * percent / 100.f);
        try {
            musicManager.seekTo(toDuration);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
