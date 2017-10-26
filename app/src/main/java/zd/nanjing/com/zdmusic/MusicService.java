package zd.nanjing.com.zdmusic;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import zd.nanjing.com.zdmusic.db.MusicDao;
import zd.nanjing.com.zdmusic.db.MusicProfile;

/**
 * 音乐服务运行在私有进程中，服务包括：
 * 1.MediaPlayer状态控制
 * 2.列表控制
 * 3.音乐焦点控制
 * 4.耳机拔出控制
 * 5.线控和MediaSession没有实现
 * 6.锁屏界面
 * 7.通知，简单实现了一下当前播放的歌曲
 * <p>
 * Created by Administrator on 2017/9/21.
 */

public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "音乐服务";


    /**
     * MediaPlayer没什么好说的，用来播放当前的音乐，这里主要是注意MediaPlayer的状态的控制，
     * 因为服务端Binder的线程池的原因会导致MediaPlayer状态混乱
     */
    private MediaPlayer mediaPlayer = new MediaPlayer();
    /**
     * 数据库管理类
     */
    private MusicDao musicDao;
    /**
     * SharedPreferences管理类
     */
    private MusicProfile musicProfile;

    /**
     * 客户端回调List
     */
    private RemoteCallbackList<MusicCallback> remoteCallbackList = new RemoteCallbackList<>();
    /**
     * 当前播放的列表
     */
    private List<Music> curMusics = new ArrayList<>();

    /**
     * 当前歌曲在当前列表中的下标索引
     */
    private int curIndex;
    /**
     * 当前播放的模式
     */
    private int mode;
    /**
     * 用于在随机播放模式下生成下一曲随机下标
     */
    private Random random = new Random();
    /**
     * 当前播放的列表id（数据库中的id）
     */
    private long curDirectoryId = Constant.LOCAL_MUSIC_DIRECTORY_ID;//-1代表的就是本地列表
    /**
     * 当前音乐服务的状态，这个很重要，用来控制MediaPlayer 的状态的
     */
    private int server_status;
    /**
     * 当前播放的音乐
     */
    private Music curMusic;
    /**
     * 用来标记音乐服务是不是刚启动(没有任何音乐播放过,就是当前音乐如果没有setDateSource过,那么客户端发起播放请求的时候,是播放新的歌曲,
     * 而不是start)
     */
    private boolean isFirstPlayMusic = true;
    /**
     * 各种服务引用
     */
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private WindowManager windowManager;
    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock keyguardLock;

    /**
     * 锁屏界面的一些空间引用
     */
    private FrameLayout lockView;
    private ImageView lockViewBg;
    private TextView lockViewTitle;
    private TextView lockViewArtist;
    private ImageView lockViewLast;
    private ImageView lockViewToggle;
    private ImageView lockViewNext;
    /**
     * 记录锁屏界面滑动的起点X坐标
     */
    private int startX;
    /**
     * 是不是锁屏的状态,用来控制windowManager 的addView操作,如果在锁屏状态就不用添加了,只有在右划退出锁屏的时候下次锁屏再来添加
     */
    private boolean hasLocked = false;
    /**
     * 当前失去音乐焦点是因为短暂失去焦点,这种情况或重新获得焦点的
     */
    private boolean isPausedByLossFocusTransient = false;
    /**
     * 失去焦点时候的音量
     */
    private int volumeWhenFocusLossTransient = 0;

    /**
     * 广播用来监听拔出耳机和屏幕关闭
     */

    private BroadcastReceiver earPotBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                if (server_status == Constant.SERVER_MODE_PLAYING) {
                    mediaPlayer.pause();
                    server_status = Constant.SERVER_MODE_PAUSE;
                    try {
                        executeCallBack(Constant.SERVER_CALLBACK_ON_PAUSE, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                /**
                 * 这里没有进行,ActivityStack栈顶Activity 的判断,如果正好是我们的音乐的话,其实不用锁屏
                 */
                keyguardLock.disableKeyguard();
                showLockView();

            }
        }
    };


    /**
     * 如果记住上次播放的列表和歌曲，那么先获取上次的列表，然后获取歌曲，通过歌曲找到上次的位置
     * 如果不记住上次播放的列表和歌曲，那么就直接拿本地音乐列表
     * 加载锁屏界面
     */
    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * 注册上面的广播
         */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(earPotBroadcastReceiver, intentFilter);

        /**
         * MediaPlayer设置资源结束监听,用于播放下一曲
         */
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    playNextMusic();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        /**
         * MediaPlayer准备完成的时候进行MediaPlayer的start操作,保证MediaPlayer的状态正确
         */
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                server_status = Constant.SERVER_MODE_PLAYING;

                /**
                 * 播放第一首歌的时候,有可能是上次退出的歌曲,所以要进行seekTo操作,因为上次保存了进度
                 */
                if (isFirstPlayMusic) {
                    isFirstPlayMusic = false;
                    if (musicProfile.isRememberLastMusic()) {
                        mp.seekTo((int) musicProfile.getLastPosition());
                    }
                }

                /**
                 * 锁屏界面设置
                 */

                lockViewTitle.setText(curMusic.getMusic_title());
                lockViewArtist.setText(curMusic.getMusic_artist());
                lockViewBg.setImageBitmap(MusicUtil.blurBitmap(AlbumImageLoader.getInstance().get(curMusic.getMusic_albumId()), MusicService.this));

                /**
                 * 发送一条通知
                 */
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MusicService.this);
                notification.setSmallIcon(R.mipmap.logo);
                notification.setTicker("正在切换歌曲");
                notification.setOngoing(true);
                notification.setLargeIcon(AlbumImageLoader.getInstance().get(curMusic.getMusic_albumId()));
                notification.setWhen(System.currentTimeMillis());
                notification.setContentTitle(curMusic.getMusic_title());
                notification.setContentText(curMusic.getMusic_artist());
                notificationManager.notify(1, notification.build());


            }
        });

        mediaPlayer.setLooping(false);
        musicDao = new MusicDao(getApplicationContext());
        musicProfile = new MusicProfile(getApplicationContext());
        mode = musicProfile.getPLayMode();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("zdmusic");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        /**
         * 获取一下本地音乐,进行备用
         */
        List<Music> localMusic = musicDao.getLocalMusics();

        if (localMusic.size() == 0) {
            try {
                localMusic = scanLocalMusics_();//有可能扫描了之后还是空的
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }


        if (localMusic.size() == 0) {
            /**
             *本地压根就没有音乐,清空两个和音乐有关的表
             *
             */
            musicDao.clearLocalMusics();
            musicDao.clearMusicIntoDirectory();

            /**
             * 在本地音乐空的情况下,重置一些记住上次位置的标记位
             */
            musicProfile.setLastMusicDirectoryId(Constant.LOCAL_MUSIC_DIRECTORY_ID);
            musicProfile.setLastMusicId(Constant.LOCAL_MUSIC_DEFAULT_ID);
            musicProfile.setLastPosition(0);
            /**
             * 这里生成一个id为-1的音乐用来通知客户端,当前没有实质的音乐存在,因为Binder里面不能返回null,又不想在onTract里面进行异常抓取所以就这么做了
             */
            curMusic = new Music();
            curMusic.setId(-1);
            curIndex = -1;
        } else {
            /**
             * 本地存在音乐
             */
            if (musicProfile.isRememberLastMusic()) {
                /**
                 * 用户勾选了记住上次位置
                 */
                if (musicProfile.getLastMusicDirectoryId() == Constant.LOCAL_MUSIC_DIRECTORY_ID) {
                    /**
                     * 如果上次记住的就是本利音乐
                     */
                    curMusics.addAll(localMusic);
                    long lastMusicId = musicProfile.getLastMusicId();
                    if (lastMusicId == Constant.LOCAL_MUSIC_DEFAULT_ID) {
                        curIndex = 0;
                    } else {
                        /**
                         * 找到上次音乐的下标
                         */
                        boolean hasFoundLastMusic = false;
                        for (int i = 0; i < curMusics.size(); i++) {
                            Music music = curMusics.get(i);
                            if (music.getId() == lastMusicId) {
                                hasFoundLastMusic = true;
                                curIndex = i;
                                break;
                            }
                        }
                        if (!hasFoundLastMusic) {
                            curIndex = 0;
                        }
                    }

                } else {
                    /**
                     * 上次保存的不是本地音乐
                     */
                    List<Music> lastMusics = musicDao.getMusicsByDirectoryId(musicProfile.getLastMusicDirectoryId());
                    if (lastMusics.size() == 0) {
                        /**
                         * 上次保存的音乐不存在了,或这歌曲被删了
                         */
                        curMusics.addAll(localMusic);
                        curIndex = 0;
                    } else {
                        /**
                         * 找到上次音乐存在的位置
                         */
                        curMusics.addAll(lastMusics);
                        long lastMusicId = musicProfile.getLastMusicId();
                        if (lastMusicId == Constant.LOCAL_MUSIC_DEFAULT_ID) {
                            curIndex = 0;
                        } else {
                            boolean hasFoundLastMusic = false;
                            for (int i = 0; i < curMusics.size(); i++) {
                                Music music = curMusics.get(i);
                                if (music.getId() == lastMusicId) {
                                    hasFoundLastMusic = true;
                                    curIndex = i;
                                    break;
                                }
                            }
                            if (!hasFoundLastMusic) {
                                curIndex = 0;
                            }
                        }
                    }
                }

            } else {
                curMusics.addAll(localMusic);
                curIndex = 0;
            }


            /**
             * 到这里curMusic有两种情况,一种是真正存在的,一种是id为-1的
             */
            curMusic = curMusics.get(curIndex);
        }


        /**
         * 锁屏界面创建,先创建出布局,避免到时候要显示的时候太耗时
         */
        lockView = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.layout_lock_screen, null);
        lockViewBg = (ImageView) lockView.findViewById(R.id.iv_bg);
        lockViewTitle = (TextView) lockView.findViewById(R.id.tv_title);
        lockViewArtist = (TextView) lockView.findViewById(R.id.tv_artist);
        lockViewLast = (ImageView) lockView.findViewById(R.id.iv_control_last);
        lockViewToggle = (ImageView) lockView.findViewById(R.id.iv_control_toggle_play);
        lockViewNext = (ImageView) lockView.findViewById(R.id.iv_control_next);
        lockViewLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    playLastMusic();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        lockViewToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    togglePlay_();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        lockViewNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    playNextMusic();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        lockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /**
                 * 利用锁屏根元素的onTouch来进行滑动的设置
                 */
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = (int) event.getX();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int deltax = (int) (event.getX() - startX);
                        if (deltax > 0)
                            lockView.setTranslationX(deltax);
                        break;
                    case MotionEvent.ACTION_UP:
                        int deltax_ = (int) (event.getX() - startX);
                        if (deltax_ > lockView.getWidth() / 2) {
                            windowManager.removeView(lockView);
                            lockView.setTranslationX(0);
                            hasLocked = false;
                        } else {
                            lockView.setTranslationX(0);
                        }
                        break;
                }
                return true;
            }
        });

        if (curMusic.getId() != Constant.LOCAL_MUSIC_DEFAULT_ID) {
            lockViewBg.setImageBitmap(MusicUtil.blurBitmap(AlbumImageLoader.getInstance().get(curMusic.getMusic_albumId()), this));
            lockViewTitle.setText(curMusic.getMusic_title());
            lockViewArtist.setText(curMusic.getMusic_artist());
        }
        lockViewToggle.setImageResource(R.drawable.ic_play_stop_music);


    }


    /**
     * MusicManager的具体实现,对于一些变量的获取,我们可以不进行多线程控制,这里要注意数据的加锁和SharedPreferences设置的加锁
     */
    private MusicManager.Stub stub = new MusicManager.Stub() {


        @Override
        public List<MusicDirectory> getMusicDirectories() throws RemoteException {
            return getMusicDirectories_();
        }

        @Override
        public void scanLocalMusics() throws RemoteException {
            scanLocalMusics_();
        }

        @Override
        public List<Music> getLocalMusics() throws RemoteException {
            return getLocalMusics_();
        }

        @Override
        public List<Music> getMusicByDirectory(MusicDirectory directory) throws RemoteException {
            return getMusicByDirectory_(directory);
        }

        @Override
        public void addMusicDirectory(MusicDirectory directory) throws RemoteException {
            addMusicDirectory_(directory);
        }

        @Override
        public void removeMusicDirectory(MusicDirectory directory) throws RemoteException {
            removeMusicDirectory_(directory);
        }

        @Override
        public void addMusic2Directory(Music music, MusicDirectory directory) throws RemoteException {
            addMusic2Directory_(music, directory);
        }

        @Override
        public void removeMusicFromDirectory(Music music, MusicDirectory directory) throws RemoteException {
            removeMusicFromDirectory_(music, directory);
        }

        @Override
        public boolean isRememberLastMusic() throws RemoteException {
            return musicProfile.isRememberLastMusic();
        }

        @Override
        public void setRememberLastMusic(boolean b) throws RemoteException {
            setRememberLastMusic_(b);
        }

        @Override
        public boolean isScanShortMusic() throws RemoteException {
            return musicProfile.isScanShortMusic();
        }

        @Override
        public void setScanShortMusic(boolean b) throws RemoteException {
            setScanShortMusic_(b);
        }

        @Override
        public void lastMusic() throws RemoteException {
            playLastMusic();
        }

        @Override
        public void nextMusic() throws RemoteException {
            playNextMusic();
        }

        @Override
        public void togglePlay() throws RemoteException {
            togglePlay_();
        }

        @Override
        public void toggleMode() throws RemoteException {
            toggleMode_();
        }


        @Override
        public int getMode() throws RemoteException {
            return mode;
        }

        @Override
        public int curStatus() throws RemoteException {
            return server_status;
        }

        @Override
        public Music curMusic() throws RemoteException {
            return curMusic;
        }

        @Override
        public void likeMusic(Music music) throws RemoteException {
            likeMusic_(music);

        }

        @Override
        public void seekTo(long position) throws RemoteException {
            seekTo_((int) position);
        }

        @Override
        public long curDuration() throws RemoteException {
            if (isFirstPlayMusic) {
                /**
                 * 没有加载过音乐的情况
                 */
                if (curMusic.getId() == Constant.LOCAL_MUSIC_DEFAULT_ID) {
                    /**
                     * 如果当前音乐空直接返回0
                     */
                    return 0;
                }
                if (musicProfile.isRememberLastMusic()) {
                    /**
                     * 如果上次有记录返回上次位置
                     */
                    return musicProfile.getLastPosition();
                } else {
                    /**
                     * 不记住上次位置就返回0
                     */
                    return 0;
                }
            } else {
                /**
                 * 有音乐加载过
                 */
                if (server_status == Constant.SERVER_MODE_PLAYING || server_status == Constant.SERVER_MODE_PAUSE) {
                    /**
                     * 暂停或者播放的时候才能获取位置,不然MediaPlayer状态会出错
                     */
                    return mediaPlayer.getCurrentPosition();
                }
                /**
                 * 其余状态都返回0
                 */
                return 0;
            }
        }

        @Override
        public void playMusic(MusicDirectory directory, int position, Music music) throws RemoteException {
            playMusic_(directory, position, music);
        }


        @Override
        public void registerCallback(MusicCallback call) throws RemoteException {
            registerCallback_(call);
        }

        @Override
        public void unregisterCallback(MusicCallback call) throws RemoteException {
            unregisterCallback_(call);
        }
    };


    /**
     * 进行客户端的回调,封装的函数,避免代码重复
     *
     * @param code
     * @param object
     * @throws RemoteException
     */
    private void executeCallBack(int code, Object object) throws RemoteException {

        /**
         * 注意RemoteCallbackList的beginBroadcast和finishBroadcast要配对使用
         */
        int n = remoteCallbackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            MusicCallback musicCallback = remoteCallbackList.getBroadcastItem(i);
            switch (code) {
                case Constant.SERVER_CALLBACK_ON_FINISH_SCAN:
                    musicCallback.onFinishScan();
                    break;
                case Constant.SERVER_CALLBACK_ON_NEW_MUSIC:
                    Music music = (Music) object;
                    musicCallback.onNewMusic(music);
                    break;
                case Constant.SERVER_CALLBACK_ON_PAUSE:
                    musicCallback.onPause();
                    break;
                case Constant.SERVER_CALLBACK_ON_PLAY:
                    musicCallback.onPlay();
                    break;
                case Constant.SERVER_CALLBACK_ON_NEW_MODE:
                    int mode = (int) object;
                    musicCallback.onNewMode(mode);
                    break;
                case Constant.SERVER_CALLBACK_ON_MUSIC_ERROR:
                    int errorCode = (int) object;
                    musicCallback.onMusicError(errorCode);
                    break;
                case Constant.SERVER_CALLBACK_ON_LIKE_MUSIC:
                    boolean success = (boolean) object;
                    musicCallback.onLikeMusic(success);
                    break;
                case Constant.SERVER_CALLBACK_ON_ADD_MUSIC_DIRECTORY:
                    boolean success1 = (boolean) object;
                    musicCallback.onAddMusicDirectory(success1);
                    break;
                case Constant.SERVER_CALLBACK_ON_REMOVE_MUSIC_DIRECTORY:
                    musicCallback.onRemoveMusicDirectory(true);
                    break;
                case Constant.SERVER_CALLBACK_ON_ADD_MUSIC_2_MUSIC_DIRECTORY:
                    boolean s = (boolean) object;
                    musicCallback.onAddMusic2MusicDirectory(s);
                    break;
                case Constant.SERVER_CALLBACK_ON_REMOVE_MUSIC_FROM_MUSIC_DIRECTORY:
                    musicCallback.onRemoveMusicFromMusicDirectory(true);
                    break;
                case Constant.SERVER_CALLBACK_ON_DIRECTORY_CHANGED:
                    musicCallback.onDirectoriesChanged();
            }
        }
        remoteCallbackList.finishBroadcast();
    }

    /**
     * 从数据库获取本地音乐列表
     *
     * @return
     */
    private synchronized List<Music> getLocalMusics_() {
        return musicDao.getLocalMusics();
    }

    private synchronized void registerCallback_(MusicCallback musicCallback) {
        remoteCallbackList.register(musicCallback);

    }

    private synchronized void unregisterCallback_(MusicCallback musicCallback) {
        remoteCallbackList.unregister(musicCallback);
    }


    /**
     * 播放上一首音乐
     *
     * @throws RemoteException
     */
    private synchronized void playLastMusic() throws RemoteException {
        if (curMusics.size() == 0) {
            return;
        }
        if (server_status == Constant.SERVER_MODE_PREPRARING || server_status == Constant.SERVER_MODE_RESET) {
            /**
             * 这里要进行状态的判断,避免MediaPlayer的状态错误
             */
            return;
        }
        /**
         * 上一首下标计算
         */
        curIndex--;
        if (curIndex < 0) {
            curIndex = curMusics.size() - 1;
        }
        curMusic = curMusics.get(curIndex);

        playNewMusic();
    }

    private synchronized void playNextMusic() throws RemoteException {
        if (curMusics.size() == 0) {
            return;
        }
        if (server_status == Constant.SERVER_MODE_PREPRARING || server_status == Constant.SERVER_MODE_RESET) {
            /**
             * 这里要进行状态的判断,避免MediaPlayer的状态错误
             */
            return;
        }
        /**
         * 下一首下标计算
         */
        switch (mode) {
            case Constant.MUSIC_MODE_ORDER:
                curIndex++;
                if (curIndex == curMusics.size()) {
                    curIndex = 0;
                }
                break;
            case Constant.MUSIC_MODE_RANDOM:
                curIndex = random.nextInt(curMusics.size());
                break;
            case Constant.MUSIC_MODE_SINGLE:
                break;
        }
        curMusic = curMusics.get(curIndex);

        playNewMusic();
    }


    /**
     * 播放新的歌曲
     *
     * @param
     */
    private synchronized void playNewMusic() throws RemoteException {


        Log.i(TAG,"当前线程id"+Thread.currentThread().getId());


        if (curMusic.getId() == Constant.LOCAL_MUSIC_DEFAULT_ID) {
            return;
        }

        if (curMusics.size() == 0) {
            return;
        }

        if (server_status == Constant.SERVER_MODE_PREPRARING || server_status == Constant.SERVER_MODE_RESET) {
            /**
             * 这里要进行状态的判断,避免MediaPlayer的状态错误
             */
            return;
        }

        /**
         * 进行音乐文件的判断如果不存在了,要从当前列表里面删除,并且还要在数据库里面删除,进行歌单变动的通知
         */
        File file = new File(curMusic.getMusic_path());
        if (!file.exists()) {
            musicDao.deleteMusicInTables(curMusic.getId());
            executeCallBack(Constant.SERVER_CALLBACK_ON_DIRECTORY_CHANGED, null);
            Toast.makeText(this, curMusic.getMusic_title() + "已经不存在", Toast.LENGTH_LONG).show();

            curMusics.remove(curIndex);
            playNextMusic();
            return;

        }

        /**
         * 先进行音乐焦点的获取
         */
        if (!requestAudioFocus()) {
            return;
        }

        /**
         * MediaPLayer重置,状态设置
         */
        mediaPlayer.reset();
        server_status = Constant.SERVER_MODE_RESET;

        try {
            /**
             * 开始准备音乐播放
             */
            mediaPlayer.setDataSource(curMusic.getMusic_path());
            server_status = Constant.SERVER_MODE_PREPRARING;
            mediaPlayer.prepare();
            executeCallBack(Constant.SERVER_CALLBACK_ON_NEW_MUSIC, curMusic);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描本地音乐
     *
     * @return
     * @throws RemoteException
     */
    private synchronized List<Music> scanLocalMusics_() throws RemoteException {
        List<Music> musics = new ArrayList<>();
        Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        BaseColumns._ID,
                        MediaStore.Audio.AudioColumns.IS_MUSIC,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.ALBUM_ID,
                        MediaStore.Audio.AudioColumns.DATA,
                        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                        MediaStore.Audio.AudioColumns.SIZE,
                        MediaStore.Audio.AudioColumns.DURATION
                },
                null,
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        while (cursor != null && cursor.moveToNext()) {
            // 是否为音乐
            int isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));

            // 持续时间
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            if (!musicProfile.isScanShortMusic() && duration < 30L * 1000) {
                continue;
            }

            if (isMusic == 0) {
                continue;
            }
            long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            // 标题
            String title = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)));
            // 艺术家
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
            // 专辑
            String album = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)));
            // 专辑封面id，根据该id可以获得专辑封面图片
            long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
            // 音乐文件路径
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
            // 音乐文件名
//            String fileName = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME)));
            // 音乐文件大小
//            long fileSize = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            Music music = new Music();
            music.setMusic_id(id);
            music.setMusic_title(title);
            music.setMusic_artist(artist);
            music.setMusic_album(album);
            music.setMusic_albumId(albumId);
            music.setMusic_duration(duration);
            music.setMusic_path(path);
            musics.add(music);
        }
        cursor.close();
        musicDao.updateLocalMusics(musics);
        executeCallBack(Constant.SERVER_CALLBACK_ON_FINISH_SCAN, null);
        return musics;
    }

    /**
     * 从数据库获取歌单列表
     *
     * @return
     */
    private synchronized List<MusicDirectory> getMusicDirectories_() {
        List<Music> localMusic = musicDao.getLocalMusics();
        MusicDirectory musicDirectory = new MusicDirectory();
        musicDirectory.setId(Constant.LOCAL_MUSIC_DIRECTORY_ID);
        musicDirectory.setMusic_directory_title("本地音乐");
        musicDirectory.setMusic_directory_musicNum(localMusic.size());
        List<MusicDirectory> musicDirectories = musicDao.getMusicDirectory();
        musicDirectories.add(0, musicDirectory);
        return musicDirectories;
    }

    /**
     * 从数据库获取歌单里面的歌曲
     *
     * @param directory
     * @return
     */
    private synchronized List<Music> getMusicByDirectory_(MusicDirectory directory) {
        if (directory.getId() == Constant.LOCAL_MUSIC_DIRECTORY_ID) {
            return musicDao.getLocalMusics();
        }

        return musicDao.getMusicsByDirectoryId(directory.getId());
    }

    /**
     * 添加音乐歌单到数据库
     *
     * @param directory
     * @throws RemoteException
     */
    private synchronized void addMusicDirectory_(MusicDirectory directory) throws RemoteException {
        boolean b = musicDao.addMusicDirectory(directory);
        executeCallBack(Constant.SERVER_CALLBACK_ON_ADD_MUSIC_DIRECTORY, b);
    }

    /**
     * 从数据库删除歌单
     *
     * @param directory
     * @throws RemoteException
     */
    private synchronized void removeMusicDirectory_(MusicDirectory directory) throws RemoteException {
        musicDao.deleteMusicDirectory(directory);
        executeCallBack(Constant.SERVER_CALLBACK_ON_REMOVE_MUSIC_DIRECTORY, null);
    }

    /**
     * 添加音乐到歌单
     *
     * @param music
     * @param directory
     * @throws RemoteException
     */
    private synchronized void addMusic2Directory_(Music music, MusicDirectory directory) throws RemoteException {
        boolean b = musicDao.addMusic2Directory(music, directory);
        executeCallBack(Constant.SERVER_CALLBACK_ON_ADD_MUSIC_2_MUSIC_DIRECTORY, b);
    }

    /**
     * 从歌单删除音乐
     *
     * @param music
     * @param directory
     * @throws RemoteException
     */
    private synchronized void removeMusicFromDirectory_(Music music, MusicDirectory directory) throws RemoteException {
        musicDao.removeMusicFromDirectory(music, directory);
        executeCallBack(Constant.SERVER_CALLBACK_ON_REMOVE_MUSIC_FROM_MUSIC_DIRECTORY, null);
    }


    private synchronized void setRememberLastMusic_(boolean b) {
        musicProfile.setRememberLastMusic(b);
    }

    private synchronized void setScanShortMusic_(boolean b) {
        musicProfile.setScanShortMusic(b);
    }


    /**
     * 暂停和播放的控制
     *
     * @throws RemoteException
     */
    private synchronized void togglePlay_() throws RemoteException {
        if (isFirstPlayMusic) {
            /**
             * 第一次加载音乐就是播放一首新的歌曲
             */
            playNewMusic();
        } else {

            /**
             * 为了保证正确性,只能在两种情况下去控制音乐,一般不会出错
             */

            if (server_status == Constant.SERVER_MODE_PLAYING) {
                server_status = Constant.SERVER_MODE_PAUSE;
                mediaPlayer.pause();
                lockViewToggle.setImageResource(R.drawable.ic_play_play_music);
                executeCallBack(Constant.SERVER_CALLBACK_ON_PAUSE, null);
                return;
            }
            if (server_status == Constant.SERVER_MODE_PAUSE) {
                server_status = Constant.SERVER_MODE_PLAYING;
                mediaPlayer.start();
                lockViewToggle.setImageResource(R.drawable.ic_play_stop_music);
                executeCallBack(Constant.SERVER_CALLBACK_ON_PLAY, null);
            }
        }
    }

    /**
     * 选择播放模式
     *
     * @throws RemoteException
     */
    private synchronized void toggleMode_() throws RemoteException {
        mode++;
        if (mode > Constant.MUSIC_MODE_SINGLE) {
            mode = Constant.MUSIC_MODE_ORDER;
        }
        musicProfile.setPlayMode(mode);
        executeCallBack(Constant.SERVER_CALLBACK_ON_NEW_MODE, mode);
    }


    /**
     * 收藏音乐
     *
     * @param music
     * @throws RemoteException
     */
    private synchronized void likeMusic_(Music music) throws RemoteException {
        boolean b = musicDao.likeMusic(music);
        executeCallBack(Constant.SERVER_CALLBACK_ON_LIKE_MUSIC, b);

    }

    /**
     * 拖拽音乐
     *
     * @param p
     */
    private synchronized void seekTo_(int p) {
        if (server_status == Constant.SERVER_MODE_PAUSE || server_status == Constant.SERVER_MODE_PLAYING)
            mediaPlayer.seekTo(p);
    }

    /**
     * 播放音乐
     *
     * @param directory
     * @param position
     * @param music
     * @throws RemoteException
     */
    private synchronized void playMusic_(MusicDirectory directory, int position, Music music) throws RemoteException {

        Log.i(TAG,"当前目录id"+curDirectoryId+"新的目录id"+directory.getId()+"新的音乐id"+music.getId());


        if (curMusic.equals(music)) {
            /**
             * 要播放的歌曲当前正在播放
             */
            return;
        }
        /**
         * 记录一下歌单列表id和歌曲id
         */
        if (musicProfile.isRememberLastMusic()) {
            musicProfile.setLastMusicDirectoryId(directory.getId());
            musicProfile.setLastMusicId((int) music.getId());
        }

        if (curDirectoryId == directory.getId()) {
            /**
             * 歌单和当前的一致
             */

        } else {
            curMusics.clear();
            curDirectoryId=directory.getId();
            if (directory.getId() == Constant.LOCAL_MUSIC_DIRECTORY_ID) {
                curMusics.addAll(musicDao.getLocalMusics());
            } else {
                curMusics.addAll(musicDao.getMusicsByDirectoryId(directory.getId()));
            }
        }
        /**
         * 这里我想不出会不会有两个位置不一样的情况
         */
        curIndex = position;
        curMusic = curMusics.get(curIndex);
        playNewMusic();
    }


    /**
     * 显示锁屏界面
     */
    private void showLockView() {
        if (hasLocked)
            return;
        else {
            hasLocked = true;
            windowManager.addView(lockView, getLayoutParams());
        }
    }

    private WindowManager.LayoutParams getLayoutParams() {
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        /**
         * type为TYPE_SYSTEM_ALERT,注意权限的申请,并且TYPE_SYSTEM_ALERT权限申请的方式属于特殊权限申请这里要注意
         */
        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        return layoutParams;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (musicProfile.isRememberLastMusic()) {
            musicProfile.setLastMusicDirectoryId(curDirectoryId);
            musicProfile.setLastMusicId(curMusics.get(curIndex).getId());
            musicProfile.setLastPosition(mediaPlayer.getCurrentPosition());
        }
        mediaPlayer.release();
        notificationManager.cancel(1);
        unregisterReceiver(earPotBroadcastReceiver);
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (musicProfile.isRememberLastMusic()) {
            musicProfile.setLastMusicDirectoryId(curDirectoryId);
            musicProfile.setLastMusicId(curMusics.get(curIndex).getId());
            musicProfile.setLastPosition(mediaPlayer.getCurrentPosition());
        }
        mediaPlayer.release();
        notificationManager.cancel(1);
        unregisterReceiver(earPotBroadcastReceiver);
        audioManager.abandonAudioFocus(this);

    }

    /**
     * 请求音乐焦点
     * @return
     */
    private boolean requestAudioFocus() {
        return audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * 音乐焦点变化回调,主要是控制一下暂停和播放,音量的设置
     * @param focusChange
     */

    @Override
    public void onAudioFocusChange(int focusChange) {
        int volume;
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                //重新获得焦点
                if (isPausedByLossFocusTransient && server_status == Constant.SERVER_MODE_PAUSE) {
                    mediaPlayer.start();
                    server_status = Constant.SERVER_MODE_PLAYING;
                    try {
                        executeCallBack(Constant.SERVER_CALLBACK_ON_PLAY, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (volumeWhenFocusLossTransient > 0 && volume == volumeWhenFocusLossTransient / 2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeWhenFocusLossTransient, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                }
                isPausedByLossFocusTransient = false;
                volumeWhenFocusLossTransient = 0;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //失去焦点
                if (server_status == Constant.SERVER_MODE_PLAYING) {
                    mediaPlayer.pause();
                    server_status = Constant.SERVER_MODE_PAUSE;
                    try {
                        executeCallBack(Constant.SERVER_CALLBACK_ON_PAUSE, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (server_status == Constant.SERVER_MODE_PLAYING) {
                    mediaPlayer.pause();
                    server_status = Constant.SERVER_MODE_PAUSE;
                    try {
                        executeCallBack(Constant.SERVER_CALLBACK_ON_PAUSE, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                isPausedByLossFocusTransient = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (server_status == Constant.SERVER_MODE_PLAYING && volume > 0) {
                    volumeWhenFocusLossTransient = volume;
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeWhenFocusLossTransient / 2, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                }
                break;
        }
    }
}
