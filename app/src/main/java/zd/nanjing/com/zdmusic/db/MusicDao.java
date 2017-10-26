package zd.nanjing.com.zdmusic.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicDirectory;

/**
 * Created by Administrator on 2017/9/20.
 */

public class MusicDao {

    private MusicDbHelper musicDbHelper;

    public MusicDao(Context context) {
        musicDbHelper = new MusicDbHelper(context);
    }

    /**
     * 扫描音乐之后重新更新本地音乐表
     *
     * @param musics
     */
    public void updateLocalMusics(List<Music> musics) {
        SQLiteDatabase sqlDatabase = null;
        String INSERT_MUSIC_SQL = "insert into table_music (music_id,music_title,music_artist,music_album,music_albumId,music_duration,music_path) values (?,?,?,?,?,?,?)";
        try {
            sqlDatabase = musicDbHelper.getWritableDatabase();
            // SQL事物控制-结束之前检测是否成功，没有成功则自动回滚
            sqlDatabase.beginTransaction();
            //先清空这张表
            sqlDatabase.execSQL(MusicDbHelper.CLEAR_TABLE_MUSIC);
            for (Music music : musics) {
                SQLiteStatement stat = sqlDatabase.compileStatement(INSERT_MUSIC_SQL);
                stat.bindLong(1, music.getMusic_id());
                stat.bindString(2, music.getMusic_title());
                stat.bindString(3, music.getMusic_artist());
                stat.bindString(4, music.getMusic_album());
                stat.bindLong(5, music.getMusic_albumId());
                stat.bindLong(6, music.getMusic_duration());
                stat.bindString(7, music.getMusic_path());
                stat.executeInsert();
            }
            // 成功
            sqlDatabase.setTransactionSuccessful();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 结束
            sqlDatabase.endTransaction();
            sqlDatabase.close();
        }
    }

    /**
     * 获取本地音乐
     *
     * @return
     */
    public List<Music> getLocalMusics() {
        SQLiteDatabase sqlDatabase = musicDbHelper.getReadableDatabase();
        Cursor cursor = sqlDatabase.query("table_music", null, null, null, null, null, null);
        List<Music> musics = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Music music = new Music();
                music.setId(cursor.getLong(0));
                music.setMusic_id(cursor.getLong(1));
                music.setMusic_title(cursor.getString(2));
                music.setMusic_artist(cursor.getString(3));
                music.setMusic_album(cursor.getString(4));
                music.setMusic_albumId(cursor.getLong(5));
                music.setMusic_duration(cursor.getLong(6));
                music.setMusic_path(cursor.getString(7));
                musics.add(music);
            } while (cursor.moveToNext());
        }
        cursor.close();
        sqlDatabase.close();
        return musics;
    }

    /**
     * 获取音乐列表
     *
     * @return
     */
    public List<MusicDirectory> getMusicDirectory() {
        SQLiteDatabase sqlDatabase = musicDbHelper.getReadableDatabase();
        Cursor cursor = sqlDatabase.rawQuery("select " +
                "table_music_directory.id," +
                "table_music_directory.music_directory_title," +
                "table_music_directory.music_directory_date," +
                "table_music_directory.music_directory_picPath," +
                "(select count(*) from table_music_into_directory where table_music_into_directory.music_directory_id = table_music_directory.id) as music_num " +
                "from table_music_directory", null);
        List<MusicDirectory> musicDirectories = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                MusicDirectory musicDirectory = new MusicDirectory();
                musicDirectory.setId(cursor.getLong(0));
                musicDirectory.setMusic_directory_title(cursor.getString(1));
                musicDirectory.setMusic_directory_date(cursor.getString(2));
                musicDirectory.setMusic_directory_picPath(cursor.getString(3));
                musicDirectory.setMusic_directory_musicNum(cursor.getInt(4));
                musicDirectories.add(musicDirectory);
            } while (cursor.moveToNext());
        }
        cursor.close();
        sqlDatabase.close();
        return musicDirectories;
    }

    /**
     * 添加歌单
     *
     * @param musicDirectory
     */
    public boolean addMusicDirectory(MusicDirectory musicDirectory) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();

        Cursor cursor = sqlDatabase.rawQuery("select * from table_music_directory where music_directory_title = ?", new String[]{musicDirectory.getMusic_directory_title()});
        if (cursor.moveToFirst()) {
            cursor.close();
            sqlDatabase.close();
            return false;
        }


        ContentValues contentValues = new ContentValues();
        contentValues.put("music_directory_title", musicDirectory.getMusic_directory_title());
        contentValues.put("music_directory_date", simpleDateFormat.format(new Date()));
        contentValues.put("music_directory_picPath", musicDirectory.getMusic_directory_picPath());
        sqlDatabase.insert("table_music_directory", null, contentValues);
        sqlDatabase.close();
        return true;

    }

    /**
     * 删除歌单
     */
    public void deleteMusicDirectory(MusicDirectory musicDirectory) {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        sqlDatabase.delete("table_music_directory", "id=?", new String[]{String.valueOf(musicDirectory.getId())});
        sqlDatabase.delete("table_music_into_directory", "music_directory_id=?", new String[]{String.valueOf(musicDirectory.getId())});
        sqlDatabase.close();
    }

    public void clearLocalMusics() {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        String sql = "delete from table_music";
        sqlDatabase.execSQL(sql);
        sqlDatabase.close();
    }

    public void clearMusicIntoDirectory() {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        sqlDatabase.execSQL(MusicDbHelper.CLEAR_TABLE_ADD_MUSIC_2_DIRECTORY);
        sqlDatabase.close();
    }


    /**
     * 把音乐添加到歌单
     */
    public boolean addMusic2Directory(Music music, MusicDirectory musicDirectory) {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        Cursor cursor = sqlDatabase.rawQuery("select * from table_music_into_directory where music_id=? and music_directory_id=?", new String[]{String.valueOf(music.getId()), String.valueOf(musicDirectory.getId())});

        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            sqlDatabase.close();
            return false;
        }


        //避免插入重复数据
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ContentValues contentValues = new ContentValues();
        contentValues.put("music_directory_id", musicDirectory.getId());
        contentValues.put("music_id", music.getId());
        contentValues.put("into_date", simpleDateFormat.format(new Date()));
        sqlDatabase.insert("table_music_into_directory", null, contentValues);

        cursor.close();
        sqlDatabase.close();
        return true;
    }

    /**
     * 把音乐从歌单删除
     */
    public void removeMusicFromDirectory(Music music, MusicDirectory musicDirectory) {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        String sql = "delete from table_music_into_directory where music_id = " + music.getId() + " and music_directory_id = " + musicDirectory.getId();
        sqlDatabase.execSQL(sql);
        Log.i("sqlite", "sql finish");
        sqlDatabase.close();
    }

    /**
     * 获取歌单音乐列表，这个要采用联结的方式查询
     *
     * @param directory_id
     * @return
     */
    public List<Music> getMusicsByDirectoryId(long directory_id) {
        SQLiteDatabase sqlDatabase = musicDbHelper.getReadableDatabase();
        Cursor cursor = sqlDatabase.rawQuery("select * from table_music left join table_music_into_directory on table_music.id = table_music_into_directory.music_id where music_directory_id = ?", new String[]{String.valueOf(directory_id)});
        List<Music> musics = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Music music = new Music();
                music.setId(cursor.getLong(0));
                music.setMusic_id(cursor.getLong(1));
                music.setMusic_title(cursor.getString(2));
                music.setMusic_artist(cursor.getString(3));
                music.setMusic_album(cursor.getString(4));
                music.setMusic_albumId(cursor.getLong(5));
                music.setMusic_duration(cursor.getLong(6));
                music.setMusic_path(cursor.getString(7));
                musics.add(music);
            } while (cursor.moveToNext());
        }
        cursor.close();
        sqlDatabase.close();
        return musics;
    }

    /**
     * 收藏音乐
     *
     * @param music
     */
    public boolean likeMusic(Music music) {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        long directory_id = 0;
        Cursor cursor = sqlDatabase.rawQuery("select * from table_music_directory where music_directory_title='我的收藏'", null);
        if (cursor != null && cursor.moveToFirst()) {
            directory_id = cursor.getLong(0);

        }
        cursor.close();

        Cursor cursor_ = sqlDatabase.rawQuery("select * from table_music_into_directory where music_id = ? and music_directory_id = ?", new String[]{String.valueOf(music.getId()), String.valueOf(directory_id)});
        if (cursor_ != null && cursor_.moveToFirst()) {
            cursor_.close();
            sqlDatabase.close();
            return false;

        }

        cursor_.close();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "insert into table_music_into_directory (music_directory_id,music_id,into_date) values ("
                + directory_id + "," + music.getId() + "," + simpleDateFormat.format(new Date()) + ")";
        sqlDatabase.execSQL(sql);
        sqlDatabase.close();

        return true;
    }


    /**
     * 在播放检测音乐的时候,如果音乐不存在硬盘里,那么就要从数据库里面删除了
     *
     * @param id
     */
    public void deleteMusicInTables(long id) {
        SQLiteDatabase sqlDatabase = musicDbHelper.getWritableDatabase();
        String deleteInLocal = "delete from table_music where id = " + id;
        String deleteInAddTable = "delete from table_music_into_directory where music_id = " + id;
        sqlDatabase.execSQL(deleteInLocal);
        sqlDatabase.execSQL(deleteInAddTable);
        sqlDatabase.close();
    }


}
