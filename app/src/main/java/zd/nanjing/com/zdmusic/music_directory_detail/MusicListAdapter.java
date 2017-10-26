package zd.nanjing.com.zdmusic.music_directory_detail;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import zd.nanjing.com.zdmusic.AlbumImageLoader;
import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.R;

/**
 * Created by Administrator on 2017/9/19.
 */

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicListViewHolder> {


    public interface OnMusicClickListener {
        void onMusicClick(int position,Music music);
        void onMusicAdd2Directory(Music music);
        void onMusicRemove(Music music);
    }

    private OnMusicClickListener onMusicClickListener;

    List<Music> musics;
    Context context;

    public void removeMusic(Music music){
        musics.remove(music);
        notifyDataSetChanged();
    }

    private boolean isLocal;

    public MusicListAdapter(Context context, List<Music> musics, OnMusicClickListener onMusicClickListener,boolean isLocal) {
        this.context = context;
        this.musics = musics;
        this.isLocal=isLocal;
        this.onMusicClickListener = onMusicClickListener;
    }

    @Override
    public MusicListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        final MusicListViewHolder musicListViewHolder = new MusicListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_music, parent, false));
        musicListViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) musicListViewHolder.itemView.getTag();
                if (onMusicClickListener != null) {
                    Music m=musics.get(position);
                    onMusicClickListener.onMusicClick(position,m);
                }
            }
        });
        musicListViewHolder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) musicListViewHolder.itemView.getTag();
                if (onMusicClickListener != null) {
                    Music m=musics.get(position);
                    onMusicClickListener.onMusicRemove(m);
                }
            }
        });
        musicListViewHolder.add2Directory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) musicListViewHolder.itemView.getTag();
                if (onMusicClickListener != null) {
                    Music m=musics.get(position);
                    onMusicClickListener.onMusicAdd2Directory(m);
                }
            }
        });
        return musicListViewHolder;
    }

    @Override
    public void onBindViewHolder(MusicListViewHolder holder, int position) {
        holder.itemView.setTag(position);
        Music music = musics.get(position);
        holder.title.setText(music.getMusic_title());
        holder.artist.setText(music.getMusic_artist());
//        音乐加载貌似有点慢
        holder.album.setImageBitmap(AlbumImageLoader.getInstance().get(music.getMusic_albumId()));


        if(isLocal){
            holder.delete.setVisibility(View.GONE);
            holder.add2Directory.setVisibility(View.VISIBLE);
        }else{
            holder.delete.setVisibility(View.VISIBLE);
            holder.add2Directory.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return musics == null ? 0 : musics.size();
    }

    public static class MusicListViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView artist;
        ImageView album;
        ImageView delete;
        ImageView add2Directory;

        public MusicListViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.tv_title);
            artist = (TextView) itemView.findViewById(R.id.tv_artist);
            album = (ImageView) itemView.findViewById(R.id.iv_album);
            delete= (ImageView) itemView.findViewById(R.id.iv_delete_music);
            add2Directory= (ImageView) itemView.findViewById(R.id.iv_add_2_directory);
        }
    }
}
