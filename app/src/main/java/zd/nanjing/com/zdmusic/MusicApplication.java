package zd.nanjing.com.zdmusic;

import android.app.Application;

/**
 * Created by wanglijun on 2017/9/30.
 */

public class MusicApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        AlbumImageLoader.getInstance().init(this);

    }
}
