// MusicManager.aidl
package zd.nanjing.com.zdmusic;

// Declare any non-default types here with import statements

import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.MusicCallback;

interface MusicManager {

    //==============================歌单歌曲操作=================================
    /**
    * 获取歌单列表
    */
    List<MusicDirectory> getMusicDirectories();

    /**
    *扫描本地音乐
    */
     void scanLocalMusics();
     /**
     *获取本地歌曲
     */
     List<Music> getLocalMusics();
     /**
     *获取歌单歌曲
     */
     List<Music> getMusicByDirectory(in MusicDirectory directory);

     /**
     *添加歌单
     */
     void  addMusicDirectory(in MusicDirectory directory);
     /**
     *删除歌单
     */
      void  removeMusicDirectory(in MusicDirectory directory);
      /**
     *添加歌曲到歌单
     */
      void addMusic2Directory(in Music music ,in MusicDirectory directory);
      /**
     *从歌单删除歌曲
     */
      void removeMusicFromDirectory(in Music music ,in MusicDirectory directory);

       //==============================标记位操作=================================
        /**
            *是否记住上次退出位置
            */
       boolean isRememberLastMusic();
       void setRememberLastMusic(in boolean b);
        /**
            *是否扫描30秒以下音乐
            */
       boolean isScanShortMusic();
       void setScanShortMusic(in boolean b);

       //==============================音乐操作=================================
        /**
            *上一曲
            */
       void lastMusic();
        /**
            *下一曲
            */
       void nextMusic();
        /**
            *控制暂停和播放
            */
       void togglePlay();
        /**
            *设置播放模式
            */
       void toggleMode();
        /**
            *获取当前模式
            */
       int getMode();
        /**
            *获取当前是否正在播放
            */
       int curStatus();
        /**
            *获取当前正在播放的音乐
            */
       Music curMusic();
        /**
            *收藏音乐
            */
       void likeMusic(in Music music);
        /**
            *拖放
            */
       void seekTo(in long postion);

       long curDuration();

       void playMusic(in MusicDirectory directory, int position,in Music music);

       //==============================回调操作=================================

       void registerCallback(in MusicCallback call);
       void unregisterCallback(in MusicCallback call);

}
