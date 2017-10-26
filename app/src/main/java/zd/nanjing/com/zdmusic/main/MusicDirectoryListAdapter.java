package zd.nanjing.com.zdmusic.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import zd.nanjing.com.zdmusic.AlbumImageLoader;
import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.R;

/**
 * Created by Administrator on 2017/9/19.
 */

public class MusicDirectoryListAdapter extends RecyclerView.Adapter<MusicDirectoryListAdapter.MusicDirectoryListViewHolder> {

    public interface OnMusicDirectoryItemClickListener {

        void onMusicDirectoryDelete(MusicDirectory musicDirectory);

        void onMusicDirectoryClick(MusicDirectory musicDirectory, View view);
    }

    private OnMusicDirectoryItemClickListener onMusicDirectoryItemClickListener;

    public void setOnMusicDirectoryItemClickListener(OnMusicDirectoryItemClickListener onMusicDirectoryItemClickListener) {
        this.onMusicDirectoryItemClickListener = onMusicDirectoryItemClickListener;
    }


    List<MusicDirectory> musicDirectories = new ArrayList<>();


    @Override
    public MusicDirectoryListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final MusicDirectoryListViewHolder musicDirectoryListViewHolder = new MusicDirectoryListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_music_directory, parent, false));
        musicDirectoryListViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (int) musicDirectoryListViewHolder.itemView.getTag();
                if (onMusicDirectoryItemClickListener != null) {
                    onMusicDirectoryItemClickListener.onMusicDirectoryClick(musicDirectories.get(position), musicDirectoryListViewHolder.pic);
                }
            }
        });
        musicDirectoryListViewHolder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) musicDirectoryListViewHolder.itemView.getTag();
                if (onMusicDirectoryItemClickListener != null) {
                    onMusicDirectoryItemClickListener.onMusicDirectoryDelete(musicDirectories.get(position));
                }
            }
        });
        return musicDirectoryListViewHolder;
    }

    @Override
    public void onBindViewHolder(MusicDirectoryListViewHolder holder, int position) {
        holder.itemView.setTag(position);
        MusicDirectory musicDirectory = musicDirectories.get(position);
        if (position == 0) {
            holder.pic.setImageResource(R.drawable.ic_main_local_music);
            holder.name.setText("本地音乐");
            holder.delete.setVisibility(View.GONE);

        } else if (position == 1) {
            holder.pic.setImageResource(R.drawable.ic_main_heart_music);
            holder.name.setText("我的收藏");
            holder.delete.setVisibility(View.GONE);
        } else {
            holder.delete.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(musicDirectory.getMusic_directory_picPath())) {
                holder.pic.setImageResource(R.drawable.img_music_directory_avart);
            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(musicDirectory.getMusic_directory_picPath());
                if (bitmap != null) {
                    holder.pic.setImageBitmap(bitmap);
                }
            }
            holder.name.setText(musicDirectory.getMusic_directory_title());
        }
        holder.count.setText(String.valueOf(musicDirectory.getMusic_directory_musicNum()));
    }


    public MusicDirectoryListAdapter(){

    }

    public void updateList(List<MusicDirectory> musicDirectories) {
        if (musicDirectories == null) return;
        this.musicDirectories.clear();
        this.musicDirectories.addAll(musicDirectories);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return musicDirectories.size();
    }

    public static class MusicDirectoryListViewHolder extends RecyclerView.ViewHolder {
        ImageView pic;
        TextView name;
        TextView count;
        ImageView delete;

        public MusicDirectoryListViewHolder(View itemView) {
            super(itemView);
            pic = (ImageView) itemView.findViewById(R.id.iv_directory_pic);
            name = (TextView) itemView.findViewById(R.id.tv_directory_name);
            count = (TextView) itemView.findViewById(R.id.tv_directory_count);
            delete = (ImageView) itemView.findViewById(R.id.iv_delete_directory);
        }
    }
}
