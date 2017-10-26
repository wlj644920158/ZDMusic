package zd.nanjing.com.zdmusic.music_directory_detail;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.transition.Transition;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import zd.nanjing.com.zdmusic.Constant;
import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicCallback;
import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.MusicManager;
import zd.nanjing.com.zdmusic.MusicService;
import zd.nanjing.com.zdmusic.MyDecoration;
import zd.nanjing.com.zdmusic.R;
import zd.nanjing.com.zdmusic.main.MainActivity;
import zd.nanjing.com.zdmusic.play.MusicPlayActivity;

/**
 * Created by Administrator on 2017/9/19.
 */

public class DirectoryDetailActivity extends AppCompatActivity implements MusicListAdapter.OnMusicClickListener {


    private RecyclerView musicRecyclerView;
    private MusicListAdapter musicListAdapter;
    private MusicManager musicManager;


    private MusicDirectory directory;
    private TextView directory_name;
    private List<Music> musics;
    private List<MusicDirectory> musicDirectories;

    private Handler handler;
    private ServiceConnection serviceConnection;
    private ImageView directory_pic;
    private MusicCallback musicCallback;
    private boolean isLocal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_detail);
        directory_pic = (ImageView) findViewById(R.id.iv_directory_pic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        directory_name = (TextView) findViewById(R.id.tv_directory_name);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        directory = getIntent().getParcelableExtra("directory");
        isLocal = directory.getId() == -1 ? true : false;
        if (!TextUtils.isEmpty(directory.getMusic_directory_picPath())) {
            Bitmap bitmap = BitmapFactory.decodeFile(directory.getMusic_directory_picPath());
            directory_pic.setImageBitmap(bitmap);
        }

        directory_name.setText(directory.getMusic_directory_title());
        musicRecyclerView = (RecyclerView) findViewById(R.id.musicList);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicRecyclerView.addItemDecoration(new MyDecoration(this, LinearLayoutManager.VERTICAL));

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constant.MSG_CONNECT_SUCCESS:
                        musicListAdapter = new MusicListAdapter(DirectoryDetailActivity.this, musics, DirectoryDetailActivity.this, isLocal);
                        musicRecyclerView.setAdapter(musicListAdapter);
                        break;
                    case Constant.MSG_ADD_MUSIC_2_DIRECTORY:
                        if ((boolean) msg.obj) {
                            Toast.makeText(DirectoryDetailActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DirectoryDetailActivity.this, "已存在", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constant.MSG_REMOVE_MUSIC_FROM_DIRECTORY:
                        if ((boolean) msg.obj) {
                            Toast.makeText(DirectoryDetailActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            musicListAdapter = new MusicListAdapter(DirectoryDetailActivity.this, musics, DirectoryDetailActivity.this, isLocal);
                            musicRecyclerView.setAdapter(musicListAdapter);
                        } else {
                            Toast.makeText(DirectoryDetailActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constant.MSG_MUSIC_DIRECTORY_CHANGED:
                        musicListAdapter = new MusicListAdapter(DirectoryDetailActivity.this, musics, DirectoryDetailActivity.this, isLocal);
                        musicRecyclerView.setAdapter(musicListAdapter);
                        break;
                }
            }
        };


        musicCallback = new MusicCallback.Stub() {


            @Override
            public void onFinishScan() throws RemoteException {

            }

            @Override
            public void onNewMusic(Music music) throws RemoteException {

            }

            @Override
            public void onPause() throws RemoteException {

            }

            @Override
            public void onPlay() throws RemoteException {

            }


            @Override
            public void onNewMode(int mode) throws RemoteException {

            }


            @Override
            public void onLikeMusic(boolean success) throws RemoteException {

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
                Message message = new Message();
                message.what = Constant.MSG_ADD_MUSIC_2_DIRECTORY;
                message.obj = success;
                handler.sendMessage(message);
            }

            @Override
            public void onRemoveMusicFromMusicDirectory(boolean success) throws RemoteException {
                musics = musicManager.getMusicByDirectory(directory);
                Message message = new Message();
                message.what = Constant.MSG_REMOVE_MUSIC_FROM_DIRECTORY;
                message.obj = success;
                handler.sendMessage(message);
            }

            @Override
            public void onDirectoriesChanged() throws RemoteException {
                musics = musicManager.getMusicByDirectory(directory);
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
                        try {
                            musicManager.registerCallback(musicCallback);
                            musics = musicManager.getMusicByDirectory(directory);

                            musicDirectories = musicManager.getMusicDirectories();
                            handler.sendEmptyMessage(Constant.MSG_CONNECT_SUCCESS);
                        } catch (RemoteException e) {

                        }
                    }
                }).start();


            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };


        Intent mIntent = new Intent(DirectoryDetailActivity.this, MusicService.class);
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
    public void onMusicClick(int position, Music music) {
        try {
            musicManager.playMusic(directory, position, music);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, MusicPlayActivity.class);
        startActivity(intent);
    }

    @Override
    public void onMusicAdd2Directory(Music music) {
        MusicAdd2DirectoryPopupWindow musicAdd2DirectoryPopupWindow = new MusicAdd2DirectoryPopupWindow(musicManager, this, musicDirectories, music);
        musicAdd2DirectoryPopupWindow.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
    }

    @Override
    public void onMusicRemove(final Music music) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示信息");
        builder.setMessage("确认将" + music.getMusic_title() + "从" + directory.getMusic_directory_title() + "删除吗");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            musicManager.removeMusicFromDirectory(music, directory);
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
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(null);
        try {
            musicManager.unregisterCallback(musicCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(serviceConnection);

    }
}
