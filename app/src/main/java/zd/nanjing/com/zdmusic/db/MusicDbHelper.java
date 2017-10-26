package zd.nanjing.com.zdmusic.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2017/9/20.
 */

public class MusicDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "music";

    /**
     * 表的名称
     * 本地音乐表                       table_music
     * 歌单表                           table_music_directory
     * 添加音乐到歌单行为表             table_music_into_directory
     */

    /**
     * table_music   的字段
     * id            数据库中编号
     * music_id      资源id
     * music_title   歌曲名
     * music_artist  作者
     * music_album   专辑名
     * music_albumId 专辑id
     * music_duration 时长
     * music_path    音乐路径
     */

    /**
     * table_music_directory    的字段
     * id                       数据库中编号
     * music_directory_title    歌单名称
     * music_directory_date     歌单创建时间
     * music_directory_picPath  歌单图片地址
     * music_directory_musicNum 歌单歌曲数量
     */


    /**
     * table_music_into_directory  的字段
     * id                          数据库编号
     * music_directory_id          歌单id
     * music_id                    音乐id
     * into_date                   添加的时间
     */

    public static final String CREATE_TABLE_MUSIC_SQL = "create table if not exists table_music (id integer primary key autoincrement," +
            "music_id integer,music_title varchar(50)," +
            "music_artist varchar(50)," +
            "music_album varchar(50)," +
            "music_albumId integer," +
            "music_duration integer," +
            "music_path varchar(50))";

    public static final String CREATE_TABLE_MUSIC_DIRECTORY_SQL = "create table if not exists table_music_directory (id integer primary key autoincrement," +
            "music_directory_title varchar(50)," +
            "music_directory_date varchar(50)," +
            "music_directory_picPath varchar(50))";

    public static final String CREATE_TABLE_MUSIC_INTO_DIRECTORY = "create table if not exists table_music_into_directory (id integer primary key autoincrement," +
            "music_directory_id integer," +
            "music_id integer," +
            "into_date varchar(50))";

    public static final String INSERT_HEART_INTO_DIRECTORY = "insert into table_music_directory (music_directory_title) values ('我的收藏')";

    public static final String CLEAR_TABLE_MUSIC = "delete from table_music";
    public static final String CLEAR_TABLE_ADD_MUSIC_2_DIRECTORY = "delete from table_music_into_directory";

    public MusicDbHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }


    public MusicDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_MUSIC_SQL);
        sqLiteDatabase.execSQL(CREATE_TABLE_MUSIC_DIRECTORY_SQL);
        sqLiteDatabase.execSQL(CREATE_TABLE_MUSIC_INTO_DIRECTORY);
        sqLiteDatabase.execSQL(INSERT_HEART_INTO_DIRECTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
