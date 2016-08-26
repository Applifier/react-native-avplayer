/**
 *
 * @providesModule AVPlayerLayer
 *
*/

'use strict';

var ReactNative = require('react-native');
var React = require('react');
var { StyleSheet, requireNativeComponent, NativeModules, View } = ReactNative;
var PropTypes = React.PropTypes;

var VideoResizeMode = require('./AVPlayerLayerResizeMode');
var { extend } = require('lodash');

var VIDEO_REF = 'video';

var AVPlayerLayer = React.createClass({
  propTypes: {
    ...View.propTypes,
    /* Wrapper component */
    resizeMode: PropTypes.string,
    playerUuid: PropTypes.string,
  },

  setNativeProps(props) {
    this.refs[VIDEO_REF].setNativeProps(props);
  },

  render() {
    var style = [styles.base, this.props.style];

    var resizeMode;
    if (this.props.resizeMode === VideoResizeMode.stretch) {
      resizeMode = NativeModules.UIManager.RCTAVPlayerLayer.ScaleToFill;
    } else if (this.props.resizeMode === VideoResizeMode.contain) {
      resizeMode = NativeModules.UIManager.RCTAVPlayerLayer.ScaleAspectFit;
    } else if (this.props.resizeMode === VideoResizeMode.cover) {
      resizeMode = NativeModules.UIManager.RCTAVPlayerLayer.ScaleAspectFill;
    } else {
      resizeMode = NativeModules.UIManager.RCTAVPlayerLayer.ScaleNone;
    }
    var player = this.props.player;
    var playerUuid = player ? player.uuid : '';
    var nativeProps = extend({}, this.props, {
      style,
      resizeMode: resizeMode,
      playerUuid: playerUuid,
    });

    return <RCTVideoView ref={VIDEO_REF} {... nativeProps} />;
  },
});

var RCTVideoView = requireNativeComponent('RCTAVPlayerLayer', AVPlayerLayer);

var styles = StyleSheet.create({
  base: {
    overflow: 'hidden',
  },
});

module.exports = AVPlayerLayer;
