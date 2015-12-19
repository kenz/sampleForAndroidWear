package org.firespeed.myapplication;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by kenz on 2015/12/06.
 */
public class Hand {
    private Bitmap mScaledBitmap;
    private Bitmap mAmbientBitmap;
    private final int mBitmapId;
    private final int mAmbientBitmapId;
    private final float mTop;
    private final float mLeft;
    private final float mCenterX;
    private final float mCenterY;
    private final float mScale;

    private static Bitmap createScaledBitmap(Resources resources, int id, float scale) {
        Bitmap original = ((BitmapDrawable) (resources.getDrawable(id))).getBitmap();
        Bitmap scaled = Bitmap.createScaledBitmap(original, (int) (original.getWidth() * scale),
                (int) (original.getHeight() * scale), true);
        return scaled;
    }

    public Hand(Resources resources, int bitmapId, int ambientBitmapId, float scale, float backgroundTop, float backgroundLeft, float left, float top, float centerX, float centerY) {
        mScaledBitmap = createScaledBitmap(resources, bitmapId, scale);
        if(bitmapId == ambientBitmapId) {
            mAmbientBitmap = mScaledBitmap;
        }else{
            mAmbientBitmap = createScaledBitmap(resources, ambientBitmapId, scale);
        }
        mLeft = left * scale + backgroundLeft;
        mTop = top * scale + backgroundTop;
        mCenterX = centerX * scale + backgroundLeft;
        mCenterY = centerY * scale + backgroundTop;
        mBitmapId = bitmapId;
        mAmbientBitmapId = ambientBitmapId;
        mScale = scale;
    }

    public void rescaleBitmap(Resources resources) {
        mScaledBitmap = createScaledBitmap(resources, mBitmapId, mScale);
        if(mBitmapId == mAmbientBitmapId){
            mAmbientBitmap = mScaledBitmap;
        }else{
            mAmbientBitmap = createScaledBitmap(resources, mAmbientBitmapId, mScale);
        }
    }

    public void draw(Canvas canvas, Paint paint, Matrix matrix, float rotate, boolean isAmbient) {
        paint.setFilterBitmap(!isAmbient);
        matrix.setTranslate(mLeft, mTop);
        matrix.postRotate(rotate, mCenterX, mCenterY);
        canvas.drawBitmap(isAmbient?mAmbientBitmap:mScaledBitmap, matrix, paint);
    }

    public boolean isRecycled() {
        return mScaledBitmap == null || mScaledBitmap.isRecycled() || mAmbientBitmap == null || mAmbientBitmap.isRecycled();
    }
}
