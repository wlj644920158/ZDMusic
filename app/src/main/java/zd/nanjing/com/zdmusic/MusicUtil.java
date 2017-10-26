package zd.nanjing.com.zdmusic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Type;

/**
 * Created by wanglijun on 2017/10/1.
 */

public class MusicUtil {


    public static int dp2px(Context context, int dp) {
        final float scale = context.getApplicationContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static String duration2String(long duration) {

        int min = (int) (duration / 1000 / 60);
        int sec = (int) (duration / 1000 % 60);

        return String.format("%d:%02d", min, sec);
    }


    public static Bitmap blurBitmap(Bitmap src, Context context) {

        Bitmap des = src.copy(Bitmap.Config.ARGB_8888, true);
        RenderScript renderScript = RenderScript.create(context);
        Allocation allocation = Allocation.createFromBitmap(renderScript, src);
        Type t = allocation.getType();
        Allocation allocation1 = Allocation.createTyped(renderScript, t);
        ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        intrinsicBlur.setRadius(24);
        intrinsicBlur.setInput(allocation);
        intrinsicBlur.forEach(allocation1);
        allocation1.copyTo(des);
        allocation.destroy();
        intrinsicBlur.destroy();
        allocation1.destroy();
        t.destroy();
        renderScript.destroy();


        Bitmap des_ = Bitmap.createBitmap(des.getWidth(),des.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(des_);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                0.3f, 0, 0, 0, 0,
                0, 0.3f, 0, 0, 0,
                0, 0, 0.3f, 0, 0,
                0, 0, 0, 1, 0,
        });
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(des, 0, 0, paint);
        return des_;

    }
}
