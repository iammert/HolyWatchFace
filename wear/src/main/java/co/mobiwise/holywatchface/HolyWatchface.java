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

package co.mobiwise.holywatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class HolyWatchface extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {


        private final String midNight = "TWELVE";

        private final String[] tensNames = {
                "",
                " TEN",
                " TWENTY",
                " THIRTY",
                " FORTY",
                " FIFTY",
        };

        private final String[] numNames = {
                "",
                "ONE",
                "TWO",
                "THREE",
                "FOUR",
                "FIVE",
                "SIX",
                "SEVEN",
                "EIGHT",
                "NINE",
                "TEN",
                "ELEVEN",
                "TWELVE",
                "THIRTEEN",
                "FOURTEEN",
                "FIFTEEN",
                "SIXTEEN",
                "SEVENTEEN",
                "EIGHTEEN",
                "NINETEEN"
        };

        String textHolyShit;
        String textFucking;
        String textMotherfucker;

        Typeface typefaceBold;
        Typeface typefaceMedium;
        Typeface typefaceLightItalic;

        Paint mTextMediumPaint;
        Paint mTextLightItalicPaint;
        Paint mTextBoldPaint;

        Paint mSecondsPaint;
        Paint mBackgroundPaint;
        RectF rectFSeconds;
        Rect textBounds;
        boolean mAmbient;
        Time mTime;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(HolyWatchface.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = HolyWatchface.this.getResources();

            textHolyShit = getResources().getString(R.string.text_holy);
            textFucking = getResources().getString(R.string.text_fucking);
            textMotherfucker = getResources().getString(R.string.text_mother);

            typefaceBold = Typeface.createFromAsset(getAssets(), "bold.ttf");
            typefaceMedium = Typeface.createFromAsset(getAssets(), "medium.ttf");
            typefaceLightItalic = Typeface.createFromAsset(getAssets(), "mediumitalic.ttf");

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mSecondsPaint = new Paint();
            mSecondsPaint.setColor(resources.getColor(R.color.color_border));
            mSecondsPaint.setStrokeWidth(30.0f);
            mSecondsPaint.setAntiAlias(true);
            mSecondsPaint.setStyle(Paint.Style.STROKE);

            mTextBoldPaint = new Paint();
            mTextBoldPaint.setColor(resources.getColor(R.color.color_text));
            mTextBoldPaint.setAntiAlias(true);
            mTextBoldPaint.setTypeface(typefaceBold);
            mTextBoldPaint.setTextSize(60.0f);

            mTextLightItalicPaint = new Paint();
            mTextLightItalicPaint.setColor(resources.getColor(R.color.color_time));
            mTextLightItalicPaint.setAntiAlias(true);
            mTextLightItalicPaint.setTypeface(typefaceLightItalic);
            mTextLightItalicPaint.setTextSize(90.0f);

            mTextMediumPaint = new Paint();
            mTextMediumPaint.setColor(resources.getColor(R.color.color_text));
            mTextMediumPaint.setAntiAlias(true);
            mTextMediumPaint.setTypeface(typefaceMedium);
            mTextMediumPaint.setTextSize(28.0f);

            textBounds = new Rect();

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
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
                if (mLowBitAmbient) {
                    mSecondsPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int centerX = bounds.centerX();

            if(rectFSeconds == null)
                rectFSeconds = new RectF(bounds);

            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            if(!isInAmbientMode()){
                for(int i = 0 ; i < mTime.second ;  i++)
                    canvas.drawArc(rectFSeconds, (i * 6) + 1 , 5 ,false, mSecondsPaint);
            }

            mTextMediumPaint.getTextBounds(textHolyShit, 0, textHolyShit.length(), textBounds);
            canvas.drawText(textHolyShit, centerX - (textBounds.width() / 2), (3 * bounds.height() / 13 + textBounds.height() / 2) * 0.9f, mTextMediumPaint);

            String hour = getHoursText(mTime.hour);
            mTextLightItalicPaint.setTextSize(60.0f);
            mTextLightItalicPaint.getTextBounds(hour, 0, hour.length(), textBounds);
            canvas.drawText(hour, centerX - (textBounds.width() / 2), (5 * bounds.height() / 13 + textBounds.height() / 2) * 0.9f, mTextLightItalicPaint);

            mTextBoldPaint.setTextSize(40.0f);
            mTextBoldPaint.getTextBounds(textFucking, 0, textFucking.length(), textBounds);
            canvas.drawText(textFucking, centerX - (textBounds.width() / 2), (7 * bounds.height() / 13 + textBounds.height() / 2) * 0.9f, mTextBoldPaint);

            String minutes = getMinutesText(mTime.minute);
            mTextLightItalicPaint.setTextSize(40.0f);
            mTextLightItalicPaint.getTextBounds(minutes, 0, minutes.length(), textBounds);
            canvas.drawText(minutes, centerX - (textBounds.width() / 2), (9 * bounds.height() / 13 + textBounds.height() / 2) * 0.87f, mTextLightItalicPaint);

            mTextMediumPaint.getTextBounds(textMotherfucker, 0, textMotherfucker.length(), textBounds);
            canvas.drawText(textMotherfucker, centerX - (textBounds.width() / 2), (10 * bounds.height() / 13 + textBounds.height() / 2) * 0.92f, mTextMediumPaint);
        }

        private String getMinutesText(int minutes){

            if(minutes < 20)
                return numNames[minutes];

            int nums = minutes % 10;
            int tens = minutes / 10;

            return tensNames[tens] + numNames[nums];
        }

        private String getHoursText(int hours){
            int hour = hours % 12;
            if(hour == 0)
                return midNight;
            return numNames[hour];
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
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
            HolyWatchface.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            HolyWatchface.this.unregisterReceiver(mTimeZoneReceiver);
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
        private final WeakReference<HolyWatchface.Engine> mWeakReference;

        public EngineHandler(HolyWatchface.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            HolyWatchface.Engine engine = mWeakReference.get();
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
