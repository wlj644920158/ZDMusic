package zd.nanjing.com.zdmusic;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2017/9/18.
 */

public class MusicDirectory implements Parcelable{

    private long id;
    private String music_directory_title;
    private String music_directory_date;
    private int music_directory_musicNum;
    private String music_directory_picPath;

    public MusicDirectory(){}

    protected MusicDirectory(Parcel in) {
        id = in.readLong();
        music_directory_title = in.readString();
        music_directory_date = in.readString();
        music_directory_musicNum = in.readInt();
        music_directory_picPath = in.readString();
    }

    public static final Creator<MusicDirectory> CREATOR = new Creator<MusicDirectory>() {
        @Override
        public MusicDirectory createFromParcel(Parcel in) {
            return new MusicDirectory(in);
        }

        @Override
        public MusicDirectory[] newArray(int size) {
            return new MusicDirectory[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMusic_directory_title() {
        return music_directory_title;
    }

    public void setMusic_directory_title(String music_directory_title) {
        this.music_directory_title = music_directory_title;
    }

    public String getMusic_directory_date() {
        return music_directory_date;
    }

    public void setMusic_directory_date(String music_directory_date) {
        this.music_directory_date = music_directory_date;
    }

    public int getMusic_directory_musicNum() {
        return music_directory_musicNum;
    }

    public void setMusic_directory_musicNum(int music_directory_musicNum) {
        this.music_directory_musicNum = music_directory_musicNum;
    }

    public String getMusic_directory_picPath() {
        return music_directory_picPath;
    }

    public void setMusic_directory_picPath(String music_directory_picPath) {
        this.music_directory_picPath = music_directory_picPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(music_directory_title);
        parcel.writeString(music_directory_date);
        parcel.writeInt(music_directory_musicNum);
        parcel.writeString(music_directory_picPath);
    }
}
