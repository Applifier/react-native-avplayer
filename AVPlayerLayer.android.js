/**
 *
 * @providesModule AVPlayerLayer
 *
*/

'use strict';

var ReactNative = require('react-native');
var React = require('react');
var { StyleSheet, requireNativeComponent, NativeModules } = ReactNative;
var PropTypes = React.PropTypes;

var VIDEO_REF = 'video';

var AVPlayerLayer = React.createClass({
  propTypes: {
    /* Wrapper component */
    resizeMode: PropTypes.string,
    playerUuid: PropTypes.string,
  },

  setNativeProps(props) {
    this.refs[VIDEO_REF].setNativeProps(props);
  },

  render() {
    return <View style={{backgroundColor: 'red'}}></View>;
  },
});

module.exports = AVPlayerLayer;
