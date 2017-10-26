package zd.nanjing.com.zdmusic.music_directory_detail;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.util.List;

import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.MusicManager;
import zd.nanjing.com.zdmusic.MusicUtil;
import zd.nanjing.com.zdmusic.MyDecoration;
import zd.nanjing.com.zdmusic.R;

/**
 * Created by wanglijun on 2017/10/3.
 */

public class MusicAdd2DirectoryPopupWindow extends PopupWindow implements MusicDirectoryListInPopWindowAdapter.OnMusicDirectoryItemClickListener {


    private MusicManager musicManager;
    private Music music;

    public MusicAdd2DirectoryPopupWindow(MusicManager musicManager, Context context, List<MusicDirectory> musicDirectories, Music music) {
        this.musicManager = musicManager;
        LayoutInflater layoutInflater = LayoutInflater.from(context.getApplicationContext());
        this.music = music;
        LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_music_2_directory, null);
        RecyclerView recyclerView = (RecyclerView) linearLayout.findViewById(R.id.music_directory_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        MusicDirectoryListInPopWindowAdapter musicDirectoryListInPopWindowAdapter = new MusicDirectoryListInPopWindowAdapter(musicDirectories);
        recyclerView.setAdapter(musicDirectoryListInPopWindowAdapter);
        musicDirectoryListInPopWindowAdapter.setOnMusicDirectoryItemClickListener(this);
        recyclerView.addItemDecoration(new MyDecoration(context, LinearLayoutManager.VERTICAL));

        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(0));
        setHeight(MusicUtil.dp2px(context, 300));
        setContentView(linearLayout);

    }

    @Override
    public void onMusicDirectoryClick(MusicDirectory musicDirectory) {
        try {
            musicManager.addMusic2Directory(music, musicDirectory);
            dismiss();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
