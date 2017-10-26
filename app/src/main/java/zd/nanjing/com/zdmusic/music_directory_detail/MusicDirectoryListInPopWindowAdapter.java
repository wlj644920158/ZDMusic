package zd.nanjing.com.zdmusic.music_directory_detail;

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

import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.R;

/**
 * Created by Administrator on 2017/9/19.
 */

public class MusicDirectoryListInPopWindowAdapter extends RecyclerView.Adapter<MusicDirectoryListInPopWindowAdapter.MusicDirectoryListnPopWindowViewHolder> {

    public interface OnMusicDirectoryItemClickListener {
        void onMusicDirectoryClick(MusicDirectory musicDirectory);
    }

    private OnMusicDirectoryItemClickListener onMusicDirectoryItemClickListener;

    public void setOnMusicDirectoryItemClickListener(OnMusicDirectoryItemClickListener onMusicDirectoryItemClickListener) {
        this.onMusicDirectoryItemClickListener = onMusicDirectoryItemClickListener;
    }


    List<MusicDirectory> musicDirectories = new ArrayList<>();


    @Override
    public MusicDirectoryListnPopWindowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final MusicDirectoryListnPopWindowViewHolder musicDirectoryListViewHolder = new MusicDirectoryListnPopWindowViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_music_directory_small, parent, false));
        musicDirectoryListViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (int) musicDirectoryListViewHolder.itemView.getTag();
                if (onMusicDirectoryItemClickListener != null) {
                    onMusicDirectoryItemClickListener.onMusicDirectoryClick(musicDirectories.get(position));
                }
            }
        });
        return musicDirectoryListViewHolder;
    }

    @Override
    public void onBindViewHolder(MusicDirectoryListnPopWindowViewHolder holder, int position) {
        holder.itemView.setTag(position);
        MusicDirectory musicDirectory = musicDirectories.get(position);
        if (position == 0) {
            holder.pic.setImageResource(R.drawable.ic_main_heart_music);
            holder.name.setText("我的收藏");
        } else {
            if (TextUtils.isEmpty(musicDirectory.getMusic_directory_picPath())) {
                holder.pic.setImageResource(R.drawable.img_music_directory_avart);
            } else {

                Bitmap bitmap = BitmapFactory.decodeFile(musicDirectory.getMusic_directory_picPath(), options);
                if (bitmap != null) {
                    holder.pic.setImageBitmap(bitmap);
                }
            }
            holder.name.setText(musicDirectory.getMusic_directory_title());
        }
        holder.count.setText(String.valueOf(musicDirectory.getMusic_directory_musicNum()));
    }

    BitmapFactory.Options options;

    public MusicDirectoryListInPopWindowAdapter(List<MusicDirectory> musicDirectories) {
        this.musicDirectories = musicDirectories;
        options = new BitmapFactory.Options();
        options.inSampleSize = 4;
    }

    @Override
    public int getItemCount() {
        return musicDirectories == null ? 0 : musicDirectories.size();
    }

    public static class MusicDirectoryListnPopWindowViewHolder extends RecyclerView.ViewHolder {
        ImageView pic;
        TextView name;
        TextView count;

        public MusicDirectoryListnPopWindowViewHolder(View itemView) {
            super(itemView);
            pic = (ImageView) itemView.findViewById(R.id.iv_directory_pic);
            name = (TextView) itemView.findViewById(R.id.tv_directory_name);
            count = (TextView) itemView.findViewById(R.id.tv_directory_count);
        }
    }
}
