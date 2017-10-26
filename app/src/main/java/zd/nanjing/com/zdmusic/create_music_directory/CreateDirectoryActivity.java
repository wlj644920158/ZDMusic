package zd.nanjing.com.zdmusic.create_music_directory;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import zd.nanjing.com.zdmusic.Constant;
import zd.nanjing.com.zdmusic.Music;
import zd.nanjing.com.zdmusic.MusicCallback;
import zd.nanjing.com.zdmusic.MusicDirectory;
import zd.nanjing.com.zdmusic.MusicManager;
import zd.nanjing.com.zdmusic.MusicService;
import zd.nanjing.com.zdmusic.R;

/**
 * Created by Administrator on 2017/9/19.
 */

public class CreateDirectoryActivity extends AppCompatActivity {

    private static final int REQUEST_PIC = 0;
    private static final int REQUEST_CROP = 1;
    private Handler handler;
    private MusicManager musicManager;
    private MusicCallback musicCallback;
    private ServiceConnection serviceConnection;
    private String picPath = null;
    private ImageView directoryPic;
    private EditText directoryTitle;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private String imageTootPath = Environment.getExternalStorageDirectory() + "/zdmusic/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_directory);

        directoryTitle = (EditText) findViewById(R.id.et_music_title);
        directoryPic = (ImageView) findViewById(R.id.iv_music_directory_pic);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constant.MSG_ADD_DIRECTORY:
                        boolean b = (boolean) msg.obj;
                        if (b) {
                            Toast.makeText(CreateDirectoryActivity.this, "歌单创建成功", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CreateDirectoryActivity.this, "歌单存在", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };

        musicCallback = new MusicCallback.Stub() {


            @Override
            public void onFinishScan() throws RemoteException {

            }

            @Override
            public void onNewMusic(Music music) throws RemoteException {

            }

            @Override
            public void onPause() throws RemoteException {

            }

            @Override
            public void onPlay() throws RemoteException {

            }


            @Override
            public void onNewMode(int mode) throws RemoteException {

            }


            @Override
            public void onLikeMusic(boolean success) throws RemoteException {

            }

            @Override
            public void onMusicError(int errorCode) throws RemoteException {

            }

            @Override
            public void onAddMusicDirectory(boolean success) throws RemoteException {
                Message message = new Message();
                message.what = Constant.MSG_ADD_DIRECTORY;
                message.obj = success;
                handler.sendMessage(message);
            }

            @Override
            public void onRemoveMusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onAddMusic2MusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onRemoveMusicFromMusicDirectory(boolean success) throws RemoteException {

            }

            @Override
            public void onDirectoriesChanged() throws RemoteException {

            }


        };
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                musicManager = MusicManager.Stub.asInterface(service);
                try {
                    musicManager.registerCallback(musicCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };


        directoryPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PIC);
            }
        });

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIC && resultCode == RESULT_OK) {
            try {
                cropImg(data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == REQUEST_CROP && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(picPath);
            directoryPic.setImageBitmap(bitmap);
        }

        if (requestCode == REQUEST_CROP && resultCode != RESULT_OK) {
            File file = new File(picPath);
            if (file.exists()) {
                file.delete();
            }
            picPath = null;
        }

    }

    private void handleImageBeforeFitkat(Intent data) {
        Uri uri = data.getData();
        picPath = getImagePath(uri, null);
    }

    private void handleImageOnKitkat(Intent data) {
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.document".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                picPath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                picPath = getImagePath(contentUri, null);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            picPath = uri.getPath();
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            picPath = getImagePath(uri, null);
        }
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }


    private void cropImg(Uri uri) throws IOException {
        picPath = imageTootPath + System.currentTimeMillis() + ".jpg";
        File rootFile = new File(imageTootPath);
        if (!rootFile.exists()) {
            rootFile.mkdir();
        }
        File imageFile = new File(picPath);
        if (imageFile.exists()) {
            imageFile.delete();
            imageFile.createNewFile();
        }
        Uri imageUri = Uri.fromFile(imageFile);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(intent, REQUEST_CROP);
    }


    private void displayImage() {
        Bitmap bitmap = BitmapFactory.decodeFile(picPath);
        directoryPic.setImageBitmap(bitmap);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_directory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_create) {
            if (TextUtils.isEmpty(directoryTitle.getText().toString())) {
                Toast.makeText(this, "歌单名称为空", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            if (picPath == null) {
                Toast.makeText(this, "未选择封面封片", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            MusicDirectory musicDirectory = new MusicDirectory();
            musicDirectory.setMusic_directory_title(directoryTitle.getText().toString());
            musicDirectory.setMusic_directory_date(simpleDateFormat.format(new Date()));
            musicDirectory.setMusic_directory_picPath(picPath);
            try {
                musicManager.addMusicDirectory(musicDirectory);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            musicManager.unregisterCallback(musicCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
