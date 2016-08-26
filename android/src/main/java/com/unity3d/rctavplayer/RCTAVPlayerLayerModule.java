package com.unity3d.rctavplayer;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.yqritc.scalablevideoview.ScalableType;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by Üstün Ergenoglu on 24/08/16.
 */
public class RCTAVPlayerLayerModule extends SimpleViewManager<RCTAVPlayerLayer>
{
    private static final String PROP_RESIZE_MODE = "resizeMode";
    private static final String PROP_PLAYER_UUID = "playerUuid";

    @Override
    public String getName()
    {
        return "RCTAVPlayerLayer";
    }

    @Override
    @Nullable
    public Map getExportedViewConstants()
    {
        return MapBuilder.of(
                "ScaleNone", Integer.toString(ScalableType.LEFT_TOP.ordinal()),
                "ScaleToFill", Integer.toString(ScalableType.FIT_XY.ordinal()),
                "ScaleAspectFit", Integer.toString(ScalableType.FIT_CENTER.ordinal()),
                "ScaleAspectFill", Integer.toString(ScalableType.CENTER_CROP.ordinal())
        );
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (RCTAVPlayerLayer.Events event : RCTAVPlayerLayer.Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @Override
    protected RCTAVPlayerLayer createViewInstance(ThemedReactContext reactContext)
    {
        return new RCTAVPlayerLayer(reactContext);
    }

    @ReactProp(name = PROP_RESIZE_MODE)
    public void setResizeMode(final RCTAVPlayerLayer avPlayerLayer, final String resizeModeOrdinalString)
    {
        avPlayerLayer.setResizeModeModifier(ScalableType.values()[Integer.parseInt(resizeModeOrdinalString)]);
    }

    @ReactProp(name = PROP_PLAYER_UUID)
    public void setPlayerUuid(final RCTAVPlayerLayer avPlayerLayer, final String playerUuid)
    {
        avPlayerLayer.setPlayerUuid(playerUuid);
    }
}
