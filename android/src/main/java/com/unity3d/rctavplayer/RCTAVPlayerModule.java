package com.unity3d.rctavplayer;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Üstün Ergenoglu on 24/08/16.
 */
public class RCTAVPlayerModule extends ReactContextBaseJavaModule
{
    private static final String TAG = RCTAVPlayerModule.class.getSimpleName();

    private ReactApplicationContext mContext;
    private static Map<String, RCTAVPlayer> mPlayers = new HashMap<>();

    public RCTAVPlayerModule(ReactApplicationContext reactContext)
    {
        super(reactContext);
        mContext = reactContext;
    }

    @Override
    public String getName()
    {
        return "AVPlayer";
    }

    @ReactMethod
    public void createVideoPlayer(String uuid)
    {
        RCTAVPlayer avPlayer = new RCTAVPlayer(mContext);
        avPlayer.setUuid(uuid);
        mPlayers.put(uuid, avPlayer);
    }

    @ReactMethod
    public void setSource(String playerUuid, ReadableMap source, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.setSource(source);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    @ReactMethod
    public void setRepeat(String playerUuid, boolean repeat, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.setRepeat(repeat);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    @ReactMethod
    public void setMuted(String playerUuid, boolean muted, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.setMuted(muted);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    @ReactMethod
    public void setVolume(String playerUuid, float volume, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.setVolume(volume);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    @ReactMethod
    public void setRate(String playerUuid, float rate, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.setRate(rate);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    @ReactMethod
    public void setSeek(String playerUuid, float seek, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.setSeek(seek);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    @ReactMethod
    public void removePlayer(String playerUuid, Callback callback)
    {
        RCTAVPlayer avPlayer = mPlayers.get(playerUuid);
        WritableArray result = Arguments.createArray();
        if (avPlayer == null)
        {
            result.pushString("ERROR: Player with uuid not found!");
            callback.invoke(result);
            return;
        }

        avPlayer.invalidate();
        mPlayers.remove(playerUuid);
        result.pushNull();
        result.pushString(playerUuid);
        callback.invoke(result);
    }

    public static RCTAVPlayer getPlayer(String uuid)
    {
        RCTAVPlayer player = mPlayers.get(uuid);
        if (player == null)
        {
            Log.e(TAG, "Failed getting player with uuid!");
        }

        return player;
    }

    public void invalidate()
    {
        for (Map.Entry<String, RCTAVPlayer> entry : mPlayers.entrySet())
        {
            entry.getValue().invalidate();
        }
        mPlayers.clear();
    }
}
