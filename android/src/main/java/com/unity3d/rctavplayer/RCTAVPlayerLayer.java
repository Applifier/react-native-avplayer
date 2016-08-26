package com.unity3d.rctavplayer;

import android.media.MediaPlayer;
import android.util.Log;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

/**
 * Created by Üstün Ergenoglu on 24/08/16.
 */
public class RCTAVPlayerLayer extends ScalableVideoView implements RCTAVPlayer.Listener
{
    private static final String TAG = RCTAVPlayerLayer.class.getSimpleName();

    private ThemedReactContext mThemedReactContext = null;
    private RCTEventEmitter mEventEmitter = null;
    private ScalableType mResizeMode = ScalableType.FIT_XY;
    private boolean mMediaPlayerValid = false;

    public enum Events
    {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_END("onVideoEnd");

        private final String mName;

        Events(final String name)
        {
            mName = name;
        }

        @Override
        public String toString()
        {
            return mName;
        }
    }

    public RCTAVPlayerLayer(ThemedReactContext themedReactContext)
    {
        super(themedReactContext);

        mThemedReactContext = themedReactContext;
        mEventEmitter = mThemedReactContext.getJSModule(RCTEventEmitter.class);

        setSurfaceTextureListener(this);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        mMediaPlayerValid = false;

        super.onDetachedFromWindow();
    }

    @Override
    public void onPrepared(RCTAVPlayer player)
    {
        mMediaPlayerValid = true;
        setResizeModeModifier(mResizeMode);
    }

    public void setResizeModeModifier(final ScalableType resizeMode)
    {
        mResizeMode = resizeMode;

        if (mMediaPlayerValid)
        {
            mThemedReactContext.runOnUiQueueThread(new Runnable()
            {
                @Override
                public void run()
                {
                    setScalableType(resizeMode);
                    invalidate();
                }
            });
        }
    }

    private void setPlayer(MediaPlayer mediaPlayer)
    {
        mMediaPlayer = mediaPlayer;
    }

    public void setPlayerUuid(String uuid)
    {
        RCTAVPlayer avPlayer = RCTAVPlayerModule.getPlayer(uuid);
        if (avPlayer == null)
        {
            Log.e(TAG, "Cannot find player with uuid: " + uuid);
            return;
        }

        MediaPlayer mp = avPlayer.getMediaPlayer();
        setPlayer(mp);
        avPlayer.addListener(this);
    }
}
