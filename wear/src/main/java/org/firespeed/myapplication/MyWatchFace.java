/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.firespeed.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import org.firespeed.both.Config;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1) / 30;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final float DESIGNED_SIZE = 512f;
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                mCalendar.setTimeInMillis(System.currentTimeMillis());
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;


        Paint mBitmapPaint;
        Paint mDrawPaint;

        boolean mAmbient;
        Calendar mCalendar;
        int mTapCount;

        private Hand mHour;
        private Hand mMinute;
        private Bitmap mBackground;
        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;
        private float mSecLength;
        private float mHoleRadius;
        private float mBackgroundTop;
        private float mBackgroundLeft;
        private float mScale;
        private Matrix mMatrix;
        private Config mConfig;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            boolean isRound = insets.isRound();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = MyWatchFace.this.getResources();

            mBitmapPaint = new Paint();
            mBitmapPaint.setColor(resources.getColor(R.color.background));
            mBitmapPaint.setFilterBitmap(true);
            mDrawPaint = new Paint();
            mDrawPaint.setColor(resources.getColor(R.color.analog_hands));
            mDrawPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mDrawPaint.setAntiAlias(true);
            mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
            mCalendar = Calendar.getInstance();
            mMatrix = new Matrix();

            mConfig = new Config(MyWatchFace.this, null);
            mConfig.connect();

        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            mConfig.disconnect();
            mConfig = null;
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mAmbient) {
                    setWakeLock();
                    mConfig.connect();
                } else {
                    mConfig.disconnect();
                }
                if (mLowBitAmbient) {
                    mDrawPaint.setAntiAlias(!inAmbientMode);
                    mBitmapPaint.setFilterBitmap(!inAmbientMode);
                }
                invalidate();
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mConfig.setIsSmooth(!mConfig.isSmooth());
                    break;
            }
            invalidate();
        }


        private Bitmap createScaledBitmap(Resources resources, int id) {
            Bitmap original = ((BitmapDrawable) (resources.getDrawable(id))).getBitmap();
            Bitmap scaled = Bitmap.createScaledBitmap(original, (int) (original.getWidth() * mScale),
                    (int) (original.getHeight() * mScale), true);
            return scaled;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            boolean isSizeChanged = canvas.getWidth() != mWidth || canvas.getHeight() != mHeight;
            if (isSizeChanged) {
                mWidth = canvas.getWidth();
                mHeight = canvas.getHeight();
                mCenterX = mWidth / 2;
                mCenterY = mHeight / 2;
                int longSize = Math.max(canvas.getWidth(), canvas.getHeight());
                mScale = longSize / DESIGNED_SIZE;
                mDrawPaint.setTextSize(48f * mScale);
                mBackgroundLeft = (mWidth - longSize) / 2f;
                mBackgroundTop = (mHeight - longSize) / 2f;

                mSecLength = (int) (200f * mScale);

                // 中央穴の半径
                mHoleRadius = mScale * 12f;

                // 縦横の取得
                Resources resources = getResources();
                mHour = new Hand(resources, R.drawable.hour, R.drawable.hour, mScale, mBackgroundTop, mBackgroundLeft, 244f, 80f, 256f, 256f);
                mMinute = new Hand(resources, R.drawable.minute, R.drawable.minute, mScale, mBackgroundTop, mBackgroundLeft, 242f, 54f, 256f, 256f);

            }

            if (isSizeChanged
                    || mBackground == null || mBackground.isRecycled()
                    || mHour == null || mHour.isRecycled()
                    || mMinute == null || mMinute.isRecycled()) {
                Resources resources = getResources();
                mBackground = createScaledBitmap(resources, R.drawable.background);
                mHour.rescaleBitmap(resources);
                mMinute.rescaleBitmap(resources);
            }

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawBitmap(mBackground, mBackgroundLeft, mBackgroundTop, mBitmapPaint);
            }
            float hourRotate = (mCalendar.get(Calendar.HOUR) + mCalendar.get(Calendar.MINUTE) / 60f) * 30;
            mHour.draw(canvas, mBitmapPaint, mMatrix, hourRotate, mAmbient);
            float minuteRotate = (mCalendar.get(Calendar.MINUTE) + mCalendar.get(Calendar.SECOND) / 60f) * 6;
            mMinute.draw(canvas, mBitmapPaint, mMatrix, minuteRotate, mAmbient);

            if (!mAmbient) {
                float secRot;
                if (mConfig.isSmooth()) {
                    secRot = (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f) / 30f * (float) Math.PI;
                } else {
                    int milliSecond = mCalendar.get(Calendar.MILLISECOND);
                    if (milliSecond <= 800) {
                        secRot = mCalendar.get(Calendar.SECOND) / 30f * (float) Math.PI;
                    } else {
                        float shift = (mCalendar.get(Calendar.MILLISECOND) - 800) / 200f;

                        secRot = (mCalendar.get(Calendar.SECOND) + shift * shift) / 30f * (float) Math.PI;
                    }
                }
                float secX = (float) Math.sin(secRot) * mSecLength;
                float secY = (float) -Math.cos(secRot) * mSecLength;
                canvas.drawLine(mCenterX, mCenterY, mCenterX + secX, mCenterY + secY, mDrawPaint);
            }
            canvas.drawCircle(mCenterX, mCenterY, mHoleRadius, mDrawPaint);
            String text = "HELLO_TEXT";
            float x = mCenterX - (mDrawPaint.measureText(text) / 2f);
            canvas.drawText(text, x, 400 * mScale, mDrawPaint);

        }

        private static final String WAKE_LOCK_TAG = "my_watch_tag";
        private static final long WAKE_LOCK_TIME = 20000l;

        private void setWakeLock() {
            ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, WAKE_LOCK_TAG).acquire(WAKE_LOCK_TIME);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                mConfig.connect();
                setWakeLock();
            } else {
                unregisterReceiver();
                mConfig.disconnect();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
