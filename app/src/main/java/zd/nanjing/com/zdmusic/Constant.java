package zd.nanjing.com.zdmusic;

/**
 * Created by Administrator on 2017/9/18.
 */

public class Constant {


    //SharedPreferences的key
    public static final String APP_PROFILE = "APP_PROFILE";
    public static final String APP_REMEMBER_LAST_MUSIC = "APP_REMEMBER_LAST_MUSIC";
    public static final String APP_SCAN_SHORT_MUSIC = "APP_SCAN_SHORT_MUSIC";
    public static final String APP_PLAY_MODE = "APP_PLAY_MODE";

    public static final String APP_LAST_MUSIC_DIRECTORY_ID = "APP_LAST_MUSIC_DIRECTORY_ID";
    public static final String APP_LAST_MUSIC_ID = "APP_LAST_MUSIC_ID";
    public static final String APP_LAST_POSITION = "APP_LAST_POSITION";

    //默认上次列表id
    public static final int LOCAL_MUSIC_DIRECTORY_ID = -1;
    //默认上次音乐id
    public static final int LOCAL_MUSIC_DEFAULT_ID = -1;

    //播放模式
    public static final int MUSIC_MODE_ORDER = 0;
    public static final int MUSIC_MODE_RANDOM = 1;
    public static final int MUSIC_MODE_SINGLE = 2;


    //handler消息类型
    public static final int MSG_ON_NEW_MUSIC = 0;
    public static final int MSG_ON_PLAY = 1;
    public static final int MSG_ON_PAUSE = 2;
    public static final int MSG_TOGGLE_MODE = 3;
    public static final int MSG_FINISH_SCAN = 4;
    public static final int MSG_CONNECT_SUCCESS = 5;
    public static final int MSG_UPDATE_TIME = 6;
    public static final int MSG_MUSIC_LIKE = 8;
    public static final int MSG_ADD_DIRECTORY = 9;
    public static final int MSG_REMOVE_MUSIC_FROM_DIRECTORY = 10;
    public static final int MSG_REMOVE_DIRECTORY = 11;
    public static final int MSG_ADD_MUSIC_2_DIRECTORY = 12;
    public static final int MSG_MUSIC_DIRECTORY_CHANGED = 13;


    //服务的状态
    public static final int SERVER_MODE_IDL = 0;
    public static final int SERVER_MODE_RESET = 1;
    public static final int SERVER_MODE_PREPRARING = 2;
    public static final int SERVER_MODE_PLAYING = 3;
    public static final int SERVER_MODE_PAUSE = 4;
    public static final int SERVER_MODE_ERROR = 5;


    //服务回调类型
    public static final int SERVER_CALLBACK_ON_FINISH_SCAN = 0;
    public static final int SERVER_CALLBACK_ON_NEW_MUSIC = 1;
    public static final int SERVER_CALLBACK_ON_PAUSE = 2;
    public static final int SERVER_CALLBACK_ON_PLAY = 3;
    public static final int SERVER_CALLBACK_ON_NEW_MODE = 4;
    public static final int SERVER_CALLBACK_ON_MUSIC_ERROR = 5;
    public static final int SERVER_CALLBACK_ON_LIKE_MUSIC = 6;
    public static final int SERVER_CALLBACK_ON_ADD_MUSIC_DIRECTORY = 7;
    public static final int SERVER_CALLBACK_ON_REMOVE_MUSIC_DIRECTORY = 8;
    public static final int SERVER_CALLBACK_ON_ADD_MUSIC_2_MUSIC_DIRECTORY = 9;
    public static final int SERVER_CALLBACK_ON_REMOVE_MUSIC_FROM_MUSIC_DIRECTORY = 10;
    public static final int SERVER_CALLBACK_ON_DIRECTORY_CHANGED = 11;


}
