// MusicCallback.aidl
package zd.nanjing.com.zdmusic;

// Declare any non-default types here with import statements

import zd.nanjing.com.zdmusic.Music;

interface MusicCallback {

    void onFinishScan();
    void onNewMusic(in Music music);
    void onPause();
    void onPlay();
    void onNewMode(in int mode);
    void onMusicError(int errorCode);



//操作会涉及到歌单的变化
    void onLikeMusic(boolean success);
    void onAddMusicDirectory(boolean success);
    void onRemoveMusicDirectory(boolean success);
    void onAddMusic2MusicDirectory(boolean success);
    void onRemoveMusicFromMusicDirectory(boolean success);
    void onDirectoriesChanged();
}
