package com.unity3d.rctavplayer;

import android.media.MediaPlayer;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Üstün Ergenoglu on 24/08/16.
 */
public class RCTAVPlayer implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener
{
    private static final String TAG = RCTAVPlayer.class.getSimpleName();

    public interface Listener
    {
        void onPrepared(RCTAVPlayer player);
        void onDestroyed();
    }

    private MediaPlayer mMediaPlayer = null;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mDeviceEventEmitter = null;
    private ReactApplicationContext mContext = null;
    private String mUuid = null;
    private Set<Listener> mListeners = new HashSet<>();

    private boolean mPaused = false;
    private boolean mRepeat = false;
    private float mRate = 0f;
    private boolean mMuted = false;
    private float mVolume = 0f;
    private boolean mMediaPlayerValid = false;
    private int mVideoDuration = 0;
    private int mVideoBufferedDuration = 0;
    private boolean mIsCompleted = false;

    private Runnable mProgressUpdateRunnable = null;
    private Handler mProgressUpdateHandler = new Handler();

    private static final String EVENT_PROP_FAST_FORWARD = "canPlayFastForward";
    private static final String EVENT_PROP_SLOW_FORWARD = "canPlaySlowForward";
    private static final String EVENT_PROP_SLOW_REVERSE = "canPlaySlowReverse";
    private static final String EVENT_PROP_REVERSE = "canPlayReverse";
    private static final String EVENT_PROP_STEP_FORWARD = "canStepForward";
    private static final String EVENT_PROP_STEP_BACKWARD = "canStepBackward";

    private static final String EVENT_PROP_DURATION = "duration";
    private static final String EVENT_PROP_PLAYABLE_DURATION = "playableDuration";
    private static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    private static final String EVENT_PROP_SEEK_TIME = "seekTime";
    private static final String EVENT_PROP_WIDTH = "width";
    private static final String EVENT_PROP_HEIGHT = "height";
    private static final String EVENT_PROP_TARGET = "target";

    private static final String EVENT_PROP_ERROR = "error";
    private static final String EVENT_PROP_WHAT = "what";
    private static final String EVENT_PROP_EXTRA = "extra";

    private static final String PROP_SRC = "src";
    private static final String PROP_SRC_URI = "uri";
    private static final String PROP_SRC_TYPE = "type";
    private static final String PROP_SRC_IS_NETWORK = "isNetwork";
    private static final String PROP_SRC_IS_ASSET = "isAsset";

    public RCTAVPlayer(ReactApplicationContext context)
    {
        mMediaPlayer = new MediaPlayer();
        mContext = context;
        mDeviceEventEmitter = mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);

        mProgressUpdateRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (mMediaPlayerValid && !mIsCompleted)
                {
                    WritableMap event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_PLAYABLE_DURATION, mVideoBufferedDuration / 1000.0); //TODO:mBufferUpdateRunnable
                    event.putString(EVENT_PROP_TARGET, mUuid);
                    mDeviceEventEmitter.emit(RCTAVPlayerLayer.Events.EVENT_PROGRESS.toString(), event);
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
            }
        };
        mProgressUpdateHandler.post(mProgressUpdateRunnable);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RCTAVPlayer");
        sb.append(" uuid: ");
        sb.append(mUuid);

        return sb.toString();
    }

    public void setUuid(String uuid)
    {
        mUuid = uuid;
    }

    public String getUuid()
    {
        return mUuid;
    }

    public boolean isMediaPlayerValid()
    {
        return mMediaPlayerValid;
    }

    public void setSource(ReadableMap source)
    {
        boolean isNetwork = source.getBoolean(PROP_SRC_IS_NETWORK);
        boolean isAsset = source.getBoolean(PROP_SRC_IS_ASSET);
        String uri = source.getString(PROP_SRC_URI);
        String type = source.getString(PROP_SRC_TYPE);
        Log.d(TAG, "Trying to open file from URI: " + uri);

        mMediaPlayerValid = false;
        mMediaPlayer.reset();
        mVideoBufferedDuration = 0;
        mVideoDuration = 0;

        try
        {
            if (isNetwork)
            {
                // Use the shared CookieManager to access the cookies
                // set by WebViews inside the same app
                CookieManager cookieManager = CookieManager.getInstance();

                Uri parsedUrl = Uri.parse(uri);
                Uri.Builder builtUrl = parsedUrl.buildUpon();

                String cookie = cookieManager.getCookie(builtUrl.build().toString());

                Map<String, String> headers = new HashMap<>();

                if (cookie != null)
                {
                    headers.put("Cookie", cookie);
                }

                mMediaPlayer.setDataSource(mContext, parsedUrl, headers);
            }
            else if (isAsset)
            {
                if (uri.startsWith("content://"))
                {
                    Uri parsedUrl = Uri.parse(uri);
                    mMediaPlayer.setDataSource(mContext, parsedUrl);
                }
                else
                {
                    mMediaPlayer.setDataSource(uri);
                }
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error loading video - " + e.getMessage());
            return;
        }

        WritableMap src = Arguments.createMap();
        src.putString(PROP_SRC_URI, uri);
        src.putString(PROP_SRC_TYPE, type);
        src.putBoolean(PROP_SRC_IS_NETWORK, isNetwork);
        WritableMap event = Arguments.createMap();
        event.putMap(PROP_SRC, src);
        event.putString(EVENT_PROP_TARGET, mUuid);
        mDeviceEventEmitter.emit(RCTAVPlayerLayer.Events.EVENT_LOAD_START.toString(), event);

        mMediaPlayer.prepareAsync();
    }

    public void setRepeat(boolean repeat)
    {
        mRepeat = repeat;
        applyModifiers();
    }

    public void setPaused(boolean paused)
    {
        mPaused = paused;

        if (!mMediaPlayerValid)
        {
            Log.w(TAG, "setPaused(" + paused + ") called with invalid media player");
            return;
        }

        if (mPaused)
        {
            if (mMediaPlayer.isPlaying())
            {
                Log.d(TAG, "Pausing playback");
                mMediaPlayer.pause();
                mIsCompleted = true;
            }
        }
        else
        {
            if (!mMediaPlayer.isPlaying())
            {
                Log.d(TAG, "Starting playback");
                mMediaPlayer.start();
                mMediaPlayer.setOnCompletionListener(this);
                mIsCompleted = false;
            }
        }
    }

    public void setSeek(float seekTime)
    {
        int msec = (int) (seekTime * 1000.0f);
        if (mMediaPlayerValid)
        {
            WritableMap event = Arguments.createMap();
            event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getCurrentPosition() / 1000.0);
            event.putDouble(EVENT_PROP_SEEK_TIME, msec / 1000.0);
            mDeviceEventEmitter.emit(RCTAVPlayerLayer.Events.EVENT_SEEK.toString(), event);

            mMediaPlayer.seekTo(msec);
            if (mIsCompleted && mVideoDuration != 0 && msec < mVideoDuration)
            {
                mIsCompleted = false;
            }
        }
    }

    public void setRate(float rate)
    {
        Log.d(TAG, "Setting rate " + mRate + " -> " + rate + "(" + mUuid + ")");
        mRate = rate;
        if (mMediaPlayerValid)
        {
            if (rate == 1.0f)
            {
                setPaused(false);
            }
            else if (rate == 0.0f)
            {
                setPaused(true);
            }
            else
            {
                Log.w(TAG, "Setting playback rate is not yet supported on Android");
            }
        }
    }

    public void setMuted(boolean muted)
    {
        mMuted = muted;
        applyModifiers();
    }

    public void setVolume(float volume)
    {
        mVolume = volume;
        applyModifiers();
    }

    private void applyModifiers()
    {
        if (mMuted)
        {
            mMediaPlayer.setVolume(0f, 0f);
        }
        else
        {
            mMediaPlayer.setVolume(mVolume, mVolume);
        }
    }

    public MediaPlayer getMediaPlayer()
    {
        return mMediaPlayer;
    }

    public void invalidate()
    {
        Log.d(TAG, "Invalidating RCTAVPlayerLayer " + mUuid);
        mMediaPlayerValid = false;
        mMediaPlayer.release();
        mMediaPlayer = null;
        mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);

        for (Listener l: mListeners)
        {
            l.onDestroyed();
        }
    }

    public void addListener(Listener listener)
    {
        mListeners.add(listener);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent)
    {
        mVideoBufferedDuration = (int) Math.round((double) (mVideoDuration * percent) / 100.0);
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        if (mRepeat)
        {
            mMediaPlayer.start();
        }
        else
        {
            mIsCompleted = true;
        }

        WritableMap event = Arguments.createMap();
        event.putString(EVENT_PROP_TARGET, mUuid);

        mDeviceEventEmitter.emit(RCTAVPlayerLayer.Events.EVENT_END.toString(), event);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        Log.e(TAG, "Error playing media. Code: " + what + " " + extra + " player uuid: " + mUuid);
        mMediaPlayerValid = false;
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, what);
        error.putInt(EVENT_PROP_EXTRA, extra);
        WritableMap event = Arguments.createMap();
        event.putMap(EVENT_PROP_ERROR, error);
        mDeviceEventEmitter.emit(RCTAVPlayerLayer.Events.EVENT_ERROR.toString(), event);
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        Log.d(TAG, "Media prepared for playing " + mUuid);
        mMediaPlayerValid = true;
        mVideoDuration = mp.getDuration();

        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, mVideoDuration / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, mp.getCurrentPosition() / 1000.0);
        event.putInt(EVENT_PROP_WIDTH, mp.getVideoWidth());
        event.putInt(EVENT_PROP_HEIGHT, mp.getVideoHeight());
        // TODO: Actually check if you can.
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_REVERSE, true);
        event.putBoolean(EVENT_PROP_REVERSE, true);
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_STEP_BACKWARD, true);
        event.putBoolean(EVENT_PROP_STEP_FORWARD, true);
        event.putString(EVENT_PROP_TARGET, mUuid);

        mDeviceEventEmitter.emit(RCTAVPlayerLayer.Events.EVENT_LOAD.toString(), event);
        applyModifiers();

        for (Listener l: mListeners)
        {
            l.onPrepared(this);
        }

        // To set the preview in the window
        setPaused(false);
        setPaused(true);
    }
}
