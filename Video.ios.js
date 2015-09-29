/**
 *
 * @providesModule VideoPlayer
 *
*/

'use strict';

var NativeVideo = require('NativeModules').Video;
var React = require('react-native');
var EventEmitter = require('eventemitter3');
var {
  DeviceEventEmitter
 } = React;

function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

class VideoPlayer extends EventEmitter {

  constructor() {
    super();
    this.uuid = guid();
    var self = this;
    NativeVideo.createVideoPlayer(this.uuid);
    DeviceEventEmitter.addListener('onVideoLoadStart', (body) => {
      if (body.target === this.uuid) {
        this.emit('loadStart', body);
      }
    });
    DeviceEventEmitter.addListener('onVideoLoad', (body) => {
      if (body.target === this.uuid) {
        this.emit('load', body);
      }
    });
    DeviceEventEmitter.addListener('onVideoError', (body) => {
      if (body.target === this.uuid) {
        this.emit('error', body);
      }
    });
    DeviceEventEmitter.addListener('onVideoProgress', (body) => {
      if (body.target === this.uuid) {
        this._currentTime = body.currentTime;
        this.emit('progress', body);
      }
    });
    DeviceEventEmitter.addListener('onVideoSeek', (body) => {
      if (body.target === this.uuid) {
        this._currentTime = body.currentTime;
        this.emit('seek', body);
      }
    });
    DeviceEventEmitter.addListener('onVideoEnd', (body) => {
      if (body.target === this.uuid) {
        this.emit('end', body);
      }
    });
    this.rate = 1;
    this.volume = 1;
    this.repeat = 1;
    this.muted = false,
    this._source = null;
  }
  set source(source) {
    var uri = source.uri;
    if (uri && uri.match(/^\//)) {
      uri = 'file://' + uri;
    }
    var isNetwork = !!(uri && uri.match(/^https?:/));
    var isAsset = !!(uri && uri.match(/^(assets-library|file):/));
    var src = {
      uri: uri,
      isNetwork,
      isAsset,
      type: source.type || 'mp4'
    };
    NativeVideo.setSource(this.uuid, src, function(err) {
      console.log("err:" + err);
    });
    this._source = source;
  }
  get source() {
    return this._source;
  }
  set repeat(repeat) {
    NativeVideo.setRepeat(this.uuid, repeat, function(err) {});
    this._repeat = repeat;
  }
  get repeat() {
    return this._repeat;
  }
  set muted(muted) {
    NativeVideo.setMuted(this.uuid, muted, function(err) {});
    this._muted = muted;
  }
  get muted() {
    return this._muted;
  }
  set volume(volume) {
    NativeVideo.setVolume(this.uuid, volume, function(err) {});
    this._volume = volume;
  }
  get volume() {
    return this._volume;
  }
  set rate(rate) {
    NativeVideo.setRate(this.uuid, rate, function(err) {});
    this._rate = rate;
  }
  get rate() {
    return this._rate;
  }
  set currentTime(seek) {
    NativeVideo.setSeek(this.uuid, seek, function(err) {});
  }
  get currentTime() {
    return this._currentTime;
  }
  release() {
    NativeVideo.removePlayer(this.uuid, function(err) {});
    // TODO remove all listener from DeviceEventEmitter/AppEventEmitter
  }
}

module.exports = VideoPlayer;