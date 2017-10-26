package zd.nanjing.com.zdmusic.db;

import android.content.Context;
import android.content.SharedPreferences;

import zd.nanjing.com.zdmusic.Constant;

/**
 * Created by Administrator on 2017/9/20.
 */

public class MusicProfile {
    private SharedPreferences sharedPreferences;

    public MusicProfile(Context context) {
        sharedPreferences = context.getSharedPreferences(Constant.APP_PROFILE, Context.MODE_PRIVATE);
    }

    public boolean isRememberLastMusic() {
        return sharedPreferences.getBoolean(Constant.APP_REMEMBER_LAST_MUSIC, true);
    }

    public void setRememberLastMusic(boolean rememberLastMusic) {
        sharedPreferences.edit().putBoolean(Constant.APP_REMEMBER_LAST_MUSIC, rememberLastMusic).commit();
    }

    public boolean isScanShortMusic() {
        return sharedPreferences.getBoolean(Constant.APP_SCAN_SHORT_MUSIC, false);
    }

    public void setScanShortMusic(boolean scanShortMusic) {
        sharedPreferences.edit().putBoolean(Constant.APP_SCAN_SHORT_MUSIC, scanShortMusic).commit();
    }

    public int getPLayMode() {
        return sharedPreferences.getInt(Constant.APP_PLAY_MODE, Constant.MUSIC_MODE_ORDER);
    }

    public void setPlayMode(int mode) {
        sharedPreferences.edit().putInt(Constant.APP_PLAY_MODE, mode).commit();
    }


    public void setLastMusicId(long index) {
        sharedPreferences.edit().putLong(Constant.APP_LAST_MUSIC_ID, index).commit();
    }

    public void setLastPosition(long position) {
        sharedPreferences.edit().putLong(Constant.APP_LAST_POSITION, position).commit();
    }


    public long getLastMusicId() {
        return sharedPreferences.getLong(Constant.APP_LAST_MUSIC_ID, Constant.LOCAL_MUSIC_DEFAULT_ID);
    }

    public long getLastPosition() {
        return sharedPreferences.getLong(Constant.APP_LAST_POSITION, 0);
    }
    public long getLastMusicDirectoryId(){
        return sharedPreferences.getLong(Constant.APP_LAST_MUSIC_DIRECTORY_ID,-1);
    }
    public void setLastMusicDirectoryId(long id){
        sharedPreferences.edit().putLong(Constant.APP_LAST_MUSIC_DIRECTORY_ID,id).commit();
    }


}
