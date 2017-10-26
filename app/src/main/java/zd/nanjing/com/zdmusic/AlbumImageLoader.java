package zd.nanjing.com.zdmusic;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.util.LruCache;

/**
 * Created by wanglijun on 2017/9/30.
 */

public class AlbumImageLoader {

    private  static  AlbumImageLoader albumImageLoader;
    private LruCache<Long,Bitmap> bitmapLruCache;
    private Application application;

    private AlbumImageLoader(){
        int maxMemory= (int) (Runtime.getRuntime().maxMemory()/1024);
        int cacheSi=maxMemory/8;
        bitmapLruCache=new LruCache<Long,Bitmap>(cacheSi){
            @Override
            protected int sizeOf(Long key, Bitmap value) {
                return value.getRowBytes()*value.getHeight()/1024;
            }
        };
    }


    public Bitmap get(long album_id){
        Bitmap bitmap=bitmapLruCache.get(album_id);
        if(bitmap==null){
            bitmap= getAlbum(album_id);
            bitmapLruCache.put(album_id,bitmap);
        }
        return bitmap;
    }


    public  void init(Application application){
        this.application=application;
    }


    public static AlbumImageLoader getInstance(){
        if(albumImageLoader==null){
            synchronized (AlbumImageLoader.class){
                if(albumImageLoader==null){
                    albumImageLoader=new AlbumImageLoader();
                }
            }
        }
        return albumImageLoader;
    }

    public  Bitmap getAlbum(long album_id){
        String strAlbums = "content://media/external/audio/albums";
        String[] projection = new String[] {android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ART };
        Cursor cur = application.getContentResolver().query(
                Uri.parse(strAlbums + "/" + Long.toString(album_id)),
                projection, null, null, null);
        String strPath = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
            cur.moveToNext();
            strPath = cur.getString(0);
        }
        cur.close();
        Bitmap bitmap= BitmapFactory.decodeFile(strPath);
        return bitmap;
    }



}
