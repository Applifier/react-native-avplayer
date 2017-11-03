package com.unity3d.rctavplayer;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;

import com.facebook.react.uimanager.ThemedReactContext;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

/**
 * Created by Üstün Ergenoglu on 24/08/16.
 */
public class RCTAVPlayerLayer extends ScalableVideoView implements RCTAVPlayer.Listener
{
    private static final String TAG = RCTAVPlayerLayer.class.getSimpleName();

    private ThemedReactContext mThemedReactContext = null;
    private ScalableType mResizeMode = ScalableType.FIT_XY;
    private RCTAVPlayer mAVPlayer = null;

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
        Log.d(TAG, "Created layer " + this);

        mThemedReactContext = themedReactContext;

        setSurfaceTextureListener(this);
    }

    @Override
    public String toString()
    {
        return Integer.toHexString(System.identityHashCode(this));
    }

    @Override
    protected void onDetachedFromWindow()
    {
        mMediaPlayer = null;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        if (mAVPlayer != null)
        {
            mMediaPlayer = mAVPlayer.getMediaPlayer();
        }
        else
        {
            return;
        }

        // To set the preview in the window
        if (mMediaPlayer != null && mAVPlayer.isMediaPlayerValid() && !mMediaPlayer.isPlaying())
        {
            mMediaPlayer.seekTo(50);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        super.onSurfaceTextureSizeChanged(surface, width, height);

        setResizeModeModifier(mResizeMode);
    }

    @Override
    public void onPrepared(RCTAVPlayer player)
    {
        setResizeModeModifier(mResizeMode);
    }

    @Override
    public void onDestroyed()
    {
        mMediaPlayer = null;
    }

    public void setResizeModeModifier(final ScalableType resizeMode)
    {
        mResizeMode = resizeMode;

        if (mAVPlayer != null && mAVPlayer.isMediaPlayerValid())
        {
            Log.d(TAG, "setResizeModifier " + mAVPlayer);
            mThemedReactContext.runOnUiQueueThread(new Runnable()
            {
                @Override
                public void run()
                {
                    // If mMediaPlayer becomes null when scrolling fast enough and this is not yet
                    // run.
                    if (mMediaPlayer != null && mAVPlayer.isMediaPlayerValid())
                    {
                        setScalableType(resizeMode);
                    }
                    else
                    {
                        Log.w(TAG, "mMediaPlayer became null before getting ready...");
                    }
                }
            });
        }
    }

    private void setPlayer(MediaPlayer mediaPlayer)
    {
        mMediaPlayer = mediaPlayer;

        if (mMediaPlayer != null && mAVPlayer != null && mAVPlayer.isMediaPlayerValid())
        {
            setResizeModeModifier(mResizeMode);
        }
    }

    public void setPlayerUuid(String uuid)
    {
        if (mAVPlayer != null)
        {
            mAVPlayer.removeListener(this);
            setPlayer(null);
            mAVPlayer = null;
        }

        Log.d(TAG, "Setting player with uuid " + uuid + " to layer " + this);
        RCTAVPlayer avPlayer = RCTAVPlayerModule.getPlayer(uuid);
        if (avPlayer == null)
        {
            Log.e(TAG, "Cannot find player with uuid: " + uuid);
            return;
        }

        mAVPlayer = avPlayer;

        MediaPlayer mp = mAVPlayer.getMediaPlayer();
        setPlayer(mp);
        mAVPlayer.addListener(this);
    }
}
