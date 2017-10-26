package zd.nanjing.com.zdmusic.main;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import zd.nanjing.com.zdmusic.AlbumImageLoader;
import zd.nanjing.com.zdmusic.Constant;
import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicCallback;
import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.MusicManager;
import zd.nanjing.com.zdmusic.MusicService;
import zd.nanjing.com.zdmusic.MyDecoration;
import zd.nanjing.com.zdmusic.R;
import zd.nanjing.com.zdmusic.create_music_directory.CreateDirectoryActivity;
import zd.nanjing.com.zdmusic.me.AboutActivity;
import zd.nanjing.com.zdmusic.music_directory_detail.DirectoryDetailActivity;
import zd.nanjing.com.zdmusic.play.MusicPlayActivity;
import zd.nanjing.com.zdmusic.setting.SettingActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MusicDirectoryListAdapter.OnMusicDirectoryItemClickListener,
        View.OnClickListener {

    private RecyclerView directoryList;
    private MusicDirectoryListAdapter musicDirectoryListAdapter;
    private ProgressDialog progressDialog;


    private ImageView music_album;
    private TextView music_title;
    private TextView music_artist;

    private ImageView control_last;
    private ImageView control_next;
    private ImageView control_toggle;
    private Music music;
    private int serverStatus;
    private List<MusicDirectory> musicDirectories;


    private Handler handler;

    private MusicCallback musicCallback;

    private MusicManager musicManager;
    private ServiceConnection serviceConnection;
    private LinearLayout musicControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        music_album = (ImageView) findViewById(R.id.iv_album);
        music_title = (TextView) findViewById(R.id.tv_title);
        music_artist = (TextView) findViewById(R.id.tv_artist);
        control_last = (ImageView) findViewById(R.id.iv_control_last);
        control_next = (ImageView) findViewById(R.id.iv_control_next);
        control_toggle = (ImageView) findViewById(R.id.iv_control_toggle_play);
        musicControl = (LinearLayout) findViewById(R.id.ll_music_control);


        control_last.setOnClickListener(this);
        control_toggle.setOnClickListener(this);
        control_next.setOnClickListener(this);
        musicControl.setOnClickListener(this);


        directoryList = (RecyclerView) findViewById(R.id.music_directory_list);
        directoryList.setLayoutManager(new LinearLayoutManager(this));
        musicDirectoryListAdapter = new MusicDirectoryListAdapter();
        musicDirectoryListAdapter.setOnMusicDirectoryItemClickListener(this);
        directoryList.setAdapter(musicDirectoryListAdapter);
        directoryList.addItemDecoration(new MyDecoration(this, LinearLayoutManager.VERTICAL));


        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constant.MSG_ON_NEW_MUSIC:
                        music_album.setImageBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()));
                        music_title.setText(music.getMusic_title());
                        music_artist.setText(music.getMusic_artist());
                        control_toggle.setImageResource(R.drawable.ic_main_stop_music);
                        break;
                    case Constant.MSG_ON_PLAY:
                        control_toggle.setImageResource(R.drawable.ic_main_stop_music);
                        break;
                    case Constant.MSG_ON_PAUSE:
                        control_toggle.setImageResource(R.drawable.ic_main_play_music);
                        break;
                    case Constant.MSG_FINISH_SCAN:
                        hideProgress();
                        musicDirectoryListAdapter.updateList(musicDirectories);
                        break;
                    case Constant.MSG_CONNECT_SUCCESS:
                        musicDirectoryListAdapter.updateList(musicDirectories);

                        if (music.getId() == Constant.LOCAL_MUSIC_DEFAULT_ID) {
                            return;
                        }

                        music_album.setImageBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()));
                        music_title.setText(music.getMusic_title());
                        music_artist.setText(music.getMusic_artist());

                        if (serverStatus == Constant.SERVER_MODE_PLAYING) {
                            control_toggle.setImageResource(R.drawable.ic_main_stop_music);
                        } else {
                            control_toggle.setImageResource(R.drawable.ic_main_play_music);
                        }
                        break;
                    case Constant.MSG_UPDATE_TIME:

                        break;
                    case Constant.MSG_MUSIC_LIKE:
                        musicDirectoryListAdapter.updateList(musicDirectories);
                        break;
                    case Constant.MSG_ADD_DIRECTORY:
                        musicDirectoryListAdapter.updateList(musicDirectories);
                        break;
                    case Constant.MSG_ADD_MUSIC_2_DIRECTORY:
                        musicDirectoryListAdapter.updateList(musicDirectories);
                        break;
                    case Constant.MSG_REMOVE_MUSIC_FROM_DIRECTORY:
                        musicDirectoryListAdapter.updateList(musicDirectories);
                        break;
                    case Constant.MSG_MUSIC_DIRECTORY_CHANGED:
                        musicDirectoryListAdapter.updateList(musicDirectories);
                        break;
                }
            }
        };


        musicCallback = new MusicCallback.Stub() {

            @Override
            public void onFinishScan() throws RemoteException {
                try {
                    musicDirectories = musicManager.getMusicDirectories();
                    handler.sendEmptyMessage(Constant.MSG_FINISH_SCAN);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
            public void onNewMode(int mode) throws RemoteException {

            }


            @Override
            public void onLikeMusic(boolean success) throws RemoteException {
                musicDirectories = musicManager.getMusicDirectories();
                handler.sendEmptyMessage(Constant.MSG_MUSIC_LIKE);
            }

            @Override
            public void onMusicError(int errorCode) throws RemoteException {

            }

            @Override
            public void onAddMusicDirectory(boolean success) throws RemoteException {
                if (success) {
                    musicDirectories = musicManager.getMusicDirectories();
                    handler.sendEmptyMessage(Constant.MSG_ADD_DIRECTORY);
                }

            }

            @Override
            public void onRemoveMusicDirectory(boolean success) throws RemoteException {
                musicDirectories = musicManager.getMusicDirectories();
                Message message = new Message();
                message.what = Constant.MSG_REMOVE_DIRECTORY;
                message.obj = success;
                handler.sendMessage(message);
            }

            @Override
            public void onAddMusic2MusicDirectory(boolean success) throws RemoteException {
                if (success)
                    musicDirectories = musicManager.getMusicDirectories();
                Message message = new Message();
                message.what = Constant.MSG_ADD_MUSIC_2_DIRECTORY;
                message.obj = success;
                handler.sendMessage(message);
            }

            @Override
            public void onRemoveMusicFromMusicDirectory(boolean success) throws RemoteException {
                musicDirectories = musicManager.getMusicDirectories();
                handler.sendEmptyMessage(Constant.MSG_REMOVE_MUSIC_FROM_DIRECTORY);
            }

            @Override
            public void onDirectoriesChanged() throws RemoteException {
                musicDirectories = musicManager.getMusicDirectories();
                handler.sendEmptyMessage(Constant.MSG_MUSIC_DIRECTORY_CHANGED);
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
                                musicDirectories = musicManager.getMusicDirectories();
                                music = musicManager.curMusic();
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


        Intent mIntent = new Intent(MainActivity.this, MusicService.class);
        bindService(mIntent, serviceConnection, BIND_AUTO_CREATE);


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            musicManager.unregisterCallback(musicCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        handler.removeCallbacks(null);
        unbindService(serviceConnection);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.san_music:
                showProgress();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            musicManager.scanLocalMusics();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.create_music_directory:
                Intent createIntent = new Intent(this, CreateDirectoryActivity.class);
                startActivity(createIntent);
                break;
            case R.id.setting:
                Intent settingIntent = new Intent(this, SettingActivity.class);
                startActivity(settingIntent);
                break;
            case R.id.about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                break;
            case R.id.exit:
                Intent mIntent = new Intent(MainActivity.this, MusicService.class);
                stopService(mIntent);
                finish();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onMusicDirectoryDelete(final MusicDirectory musicDirectory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示信息");
        builder.setMessage("确认删除歌单" + musicDirectory.getMusic_directory_title() + "吗");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            musicManager.removeMusicDirectory(musicDirectory);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();

    }

    @Override
    public void onMusicDirectoryClick(MusicDirectory musicDirectory, View view) {
        Intent intent = new Intent(this, DirectoryDetailActivity.class);
        intent.putExtra("directory", musicDirectory);
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this, view, "directoryPic").toBundle());
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this, null, "正在扫描...");
        } else {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onClick(final View v) {


        if (v.getId() == R.id.ll_music_control) {
            Intent intent = new Intent(this, MusicPlayActivity.class);
            startActivity(intent);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (v.getId()) {
                        case R.id.iv_control_last:
                            musicManager.lastMusic();
                            break;
                        case R.id.iv_control_toggle_play:
                            musicManager.togglePlay();
                            break;
                        case R.id.iv_control_next:
                            musicManager.nextMusic();
                            break;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }
}
