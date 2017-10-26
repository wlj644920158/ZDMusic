package zd.nanjing.com.zdmusic;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2017/9/18.
 */

public class Music implements Parcelable {
    private long id;
    private long music_id;
    private String music_title;
    private String music_artist;
    private String music_album;
    private long music_albumId;
    private long music_duration;
    private String music_path;


    public Music() {
    }

    protected Music(Parcel in) {
        id = in.readLong();
        music_id = in.readLong();
        music_title = in.readString();
        music_artist = in.readString();
        music_album = in.readString();
        music_albumId = in.readLong();
        music_duration = in.readLong();
        music_path = in.readString();
    }

    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(Parcel in) {
            return new Music(in);
        }

        @Override
        public Music[] newArray(int size) {
            return new Music[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMusic_id() {
        return music_id;
    }

    public void setMusic_id(long music_id) {
        this.music_id = music_id;
    }

    public String getMusic_title() {
        return music_title;
    }

    public void setMusic_title(String music_title) {
        this.music_title = music_title;
    }

    public String getMusic_artist() {
        return music_artist;
    }

    public void setMusic_artist(String music_artist) {
        this.music_artist = music_artist;
    }

    public String getMusic_album() {
        return music_album;
    }

    public void setMusic_album(String music_album) {
        this.music_album = music_album;
    }

    public long getMusic_albumId() {
        return music_albumId;
    }

    public void setMusic_albumId(long music_albumId) {
        this.music_albumId = music_albumId;
    }

    public long getMusic_duration() {
        return music_duration;
    }

    public void setMusic_duration(long music_duration) {
        this.music_duration = music_duration;
    }

    public String getMusic_path() {
        return music_path;
    }

    public void setMusic_path(String music_path) {
        this.music_path = music_path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(music_id);
        parcel.writeString(music_title);
        parcel.writeString(music_artist);
        parcel.writeString(music_album);
        parcel.writeLong(music_albumId);
        parcel.writeLong(music_duration);
        parcel.writeString(music_path);
    }

    @Override
    public String toString() {
        return this.music_path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;//自反性
        if (o == null || getClass() != o.getClass()) return false;//非空性，对称性

        Music music = (Music) o;

        if (id != music.id) return false;
        if (music_id != music.music_id) return false;
        if (music_albumId != music.music_albumId) return false;
        if (music_duration != music.music_duration) return false;
        if (music_title != null ? !music_title.equals(music.music_title) : music.music_title != null)
            return false;
        if (music_artist != null ? !music_artist.equals(music.music_artist) : music.music_artist != null)
            return false;
        if (music_album != null ? !music_album.equals(music.music_album) : music.music_album != null)
            return false;
        return music_path.equals(music.music_path);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (music_id ^ (music_id >>> 32));
        result = 31 * result + (music_title != null ? music_title.hashCode() : 0);
        result = 31 * result + (music_artist != null ? music_artist.hashCode() : 0);
        result = 31 * result + (music_album != null ? music_album.hashCode() : 0);
        result = 31 * result + (int) (music_albumId ^ (music_albumId >>> 32));
        result = 31 * result + (int) (music_duration ^ (music_duration >>> 32));
        result = 31 * result + music_path.hashCode();
        return result;
    }
}
