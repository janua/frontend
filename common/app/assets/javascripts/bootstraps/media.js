/* global videojs */
define([
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/detect',
    'common/utils/config',
    'common/utils/deferToAnalytics',
    'common/utils/url',
    'common/modules/ui/images',
    'common/modules/commercial/dfp',
    'common/modules/analytics/omnitureMedia',
    'lodash/functions/throttle',
    'lodash/objects/isFunction',
    'bean',
    'bonzo',
    'common/modules/component',
    'common/modules/analytics/beacon',
    'common/modules/ui/message',
    'raven'
], function(
    $,
    ajax,
    detect,
    config,
    deferToAnalytics,
    urlUtils,
    images,
    dfp,
    OmnitureMedia,
    _throttle,
    isFunction,
    bean,
    bonzo,
    Component,
    beacon,
    Message,
    raven
) {

    var autoplay = config.isMedia && /desktop|wide/.test(detect.getBreakpoint()),
        QUARTILES = [25, 50, 75],
        // Advert and content events used by analytics. The expected order of bean events is:
        EVENTS = [
            'preroll:request',
            'preroll:ready',
            'preroll:play',
            'preroll:end',
            'content:ready',
            'content:play',
            'content:end'
        ];

    function getMediaType(player) {
        var mediaEl = player && isFunction(player.el) ? player.el().children[0] : undefined;
        return mediaEl ? mediaEl.tagName.toLowerCase() : 'video';
    }

    function constructEventName(eventName, player) {
        return getMediaType(player) + ':' + eventName;
    }

    var modules = {

        ophanRecord: function(id, event, player) {
            if(id) {
                require('ophan/ng', function (ophan) {
                    var eventObject = {};
                    eventObject[getMediaType(player)] = {
                        id: id,
                        eventType: event.type
                    };
                    ophan.record(eventObject);
                });
            }
        },

        initOphanTracking: function(player, mediaId) {
            EVENTS.concat(QUARTILES.map(function(q) {
                return 'play:' + q;
            })).forEach(function(event) {
                player.one(constructEventName(event, player), function(event) {
                    modules.ophanRecord(mediaId, event, player);
                });
            });
        },

        initOmnitureTracking: function(player) {
            new OmnitureMedia(player).init();
        },

        bindDiagnosticsEvents: function(player) {
            player.on(constructEventName('preroll:play', player), function(){
                beacon.fire('/count/vps.gif');
            });
            player.on(constructEventName('preroll:end', player), function(){
                beacon.fire('/count/vpe.gif');
            });
            player.on(constructEventName('content:play', player), function(){
                beacon.fire('/count/vs.gif');
            });
            player.on(constructEventName('content:end', player), function(){
                beacon.fire('/count/ve.gif');
            });

            // count the number of video starts that happen after a preroll
            player.on(constructEventName('preroll:play', player), function(){
                player.on(constructEventName('content:play', player), function(){
                    beacon.fire('/count/vsap.gif');
                });
            });
        },

        bindPrerollEvents: function(player) {
            var events = {
                end: function() {
                    player.trigger(constructEventName('preroll:end', player));
                    modules.bindContentEvents(player, true);
                },
                play: function() {
                    var duration = player.duration();
                    if (duration) {
                        player.trigger(constructEventName('preroll:play', player));
                    } else {
                        player.one('durationchange', events.play);
                    }
                },
                ready: function() {
                    player.trigger(constructEventName('preroll:ready', player));

                    player.one('adstart', events.play);
                    player.one('adend', events.end);

                    if (autoplay) {
                        player.play();
                    }
                }
            };
            player.one('adsready', events.ready);

            //If no preroll avaliable or preroll fails, still init content tracking
            player.one('adtimeout', function() {
                modules.bindContentEvents(player);
            });
        },

        bindContentEvents: function(player) {
            var events = {
                end: function() {
                    player.trigger(constructEventName('content:end', player));
                },
                play: function() {
                    var duration = player.duration();
                    if (duration) {
                        player.trigger(constructEventName('content:play', player));
                    } else {
                        player.one('durationchange', events.play);
                    }
                },
                timeupdate: function() {
                    var progress = Math.round(parseInt(player.currentTime()/player.duration()*100, 10));
                    QUARTILES.reverse().some(function(quart) {
                        if (progress >= quart) {
                            player.trigger(constructEventName('play:' + quart, player));
                            return true;
                        } else {
                            return false;
                        }
                    });
                },
                ready: function() {
                    player.trigger(constructEventName('content:ready', player));

                    player.one('play', events.play);
                    player.on('timeupdate', _throttle(events.timeupdate, 1000));
                    player.one('ended', events.end);

                    if (autoplay) {
                        player.play();
                    }
                }
            };
            events.ready();
        },

        beaconError: function(err) {
            if(err && 'message' in err) {
                raven.captureException(new Error(err.message), {
                    tags: {
                        feature: 'player',
                        code: err.code
                    }
                });
            }
        },

        handleInitialMediaError: function(player) {
            var err = player.error();
            if(err !== null) {
                modules.beaconError(err);
                return err.code === 4;
            }
            return false;
        },

        bindErrorHandler: function(player) {
            player.on('error', function(e){
                modules.beaconError(e);
            });
        },

        getVastUrl: function() {
            var adUnit = config.page.adUnit,
                custParams = urlUtils.constructQuery(dfp.buildPageTargeting({ page: config.page })),
                encodedCustParams = encodeURIComponent(custParams),
                timestamp = new Date().getTime();
            return 'http://' + config.page.dfpHost + '/gampad/ads?correlator=' + timestamp + '&gdfp_req=1&env=vp&impl=s&output=' +
                    'xml_vast2&unviewed_position_start=1&iu=' + adUnit + '&sz=400x300&scp=slot%3Dvideo&cust_params=' + encodedCustParams;
        },

        countDown: function() {
            var player = this,
                tmp = '<div class="vjs-ads-overlay js-ads-overlay">Your video will start in <span class="vjs-ads-overlay__remaining js-remaining-time"></span>' +
                      ' seconds <span class="vjs-ads-overlay__label">Advertisement</span></div>',
                events =  {
                    destroy: function() {
                        $('.js-ads-overlay', this.el()).remove();
                        this.off('timeupdate', events.update);
                    },
                    update: function() {
                        $('.js-remaining-time', this.el()).text(parseInt(this.duration() - this.currentTime(), 10).toFixed());
                    },
                    init: function() {
                        $(this.el()).append($.create(tmp));
                        this.on('timeupdate', events.update.bind(this));
                        this.one(constructEventName('preroll:end', player), events.destroy.bind(player));
                        this.one(constructEventName('content:play', player), events.destroy.bind(player));
                        this.one('adtimeout', events.destroy.bind(player));
                    }
                };
            this.one(constructEventName('preroll:play', player), events.init.bind(player));
        },

        fullscreener: function() {
            var player = this,
                clickbox = bonzo.create('<div class="vjs-fullscreen-clickbox"></div>')[0],
                events = {
                    click: function(e) {
                        this.paused() ? this.play() : this.pause();
                        e.stop();
                    },
                    dblclick: function(e) {
                        e.stop();
                        this.isFullScreen() ? this.exitFullscreen() : this.requestFullscreen();
                    }
                };

            bonzo(clickbox)
                .appendTo(player.contentEl());

            bean.on(clickbox, 'click', events.click.bind(player));
            bean.on(clickbox, 'dblclick', events.dblclick.bind(player));
        },

        initLoadingSpinner: function(player) {
            player.loadingSpinner.contentEl().innerHTML =
                '<div class="pamplemousse">' +
                '<div class="pamplemousse__pip"><i></i></div>' +
                '<div class="pamplemousse__pip"><i></i></div>' +
                '<div class="pamplemousse__pip"><i></i></div>' +
                '</div>';
        },

        createVideoObject: function(el, options) {
            var vjs;

            options.techOrder = ['html5', 'flash'];
            vjs = videojs(el, options);

            if(modules.handleInitialMediaError(vjs)){
                vjs.dispose();
                options.techOrder = ['flash', 'html5'];
                vjs = videojs(el, options);
            }

            return vjs;
        },

        initPlayer: function() {

            require('bootstraps/video-player', function () {

                videojs.plugin('adCountDown', modules.countDown);
                videojs.plugin('fullscreener', modules.fullscreener);

                $('.js-gu-media').each(function (el) {
                    var mediaType = el.tagName.toLowerCase();

                    bonzo(el).addClass('vjs');

                    var mediaId = el.getAttribute('data-media-id'),
                        vjs = modules.createVideoObject(el, {
                            controls: true,
                            autoplay: false,
                            preload: 'metadata' // preload='none' & autoplay breaks ad loading on chrome35
                        });

                    //Location of this is important
                    modules.handleInitialMediaError(vjs);

                    vjs.ready(function () {
                        var player = this;

                        modules.bindErrorHandler(player);
                        modules.initLoadingSpinner(player);

                        // unglitching the volume on first load
                        var vol = vjs.volume();
                        if (vol) {
                            vjs.volume(0);
                            vjs.volume(vol);
                        }

                        vjs.persistvolume({namespace: 'gu.vjs'});

                        deferToAnalytics(function () {

                            modules.initOmnitureTracking(player);
                            modules.initOphanTracking(player, mediaId);

                            // preroll for videos only
                            if (mediaType === 'video') {

                                modules.bindDiagnosticsEvents(player);
                                player.fullscreener();

                                // Init plugins
                                if (config.switches.videoAdverts && !config.page.blockVideoAds) {
                                    modules.bindPrerollEvents(player);
                                    player.adCountDown();
                                    player.trigger(constructEventName('preroll:request', player));
                                    player.ads({
                                        timeout: 3000
                                    });
                                    player.vast({
                                        url: modules.getVastUrl(),
                                        vidFormats: ['video/mp4', 'video/webm', 'video/ogv', 'video/x-flv']
                                    });
                                } else {
                                    modules.bindContentEvents(player);
                                }

                                if (/desktop|wide/.test(detect.getBreakpoint())) {
                                    modules.initEndSlate(player, el.getAttribute('data-end-slate'));
                                }
                            } else {
                                vjs.playlist({
                                    mediaType: 'audio',
                                    continuous: false
                                });

                                modules.bindContentEvents(player);
                            }
                        });
                    });

                    // built in vjs-user-active is buggy so using custom implementation
                    var timeout;
                    vjs.on('mousemove', function() {
                        if (timeout) {
                            clearTimeout(timeout);
                        } else {
                            vjs.addClass('vjs-mousemoved');
                        }
                        timeout = setTimeout(function() {
                            vjs.removeClass('vjs-mousemoved');
                            timeout = false;
                        }, 500);
                    });
                });
            });
        },
        generateEndSlateUrlFromPage: function() {
            var seriesId = config.page.seriesId;
            var sectionId = config.page.section;
            var url = (seriesId)  ? '/video/end-slate/series/' + seriesId : '/video/end-slate/section/' + sectionId;
            return url + '.json?shortUrl=' + config.page.shortUrl;
        },
        initEndSlate: function(player, endSlatePath) {
            var endSlate = new Component(),
                endState = 'vjs-has-ended';

            endSlate.endpoint = endSlatePath || modules.generateEndSlateUrlFromPage();
            endSlate.fetch(player.el(), 'html');

            player.one(constructEventName('content:play', player), function() {
                player.on('ended', function () {
                    bonzo(player.el()).addClass(endState);
                });
            });
            player.on('playing', function() {
                bonzo(player.el()).removeClass(endState);
            });
        },
        initMoreInSection: function() {
            var section = new Component(),
                parentEl = $('.js-onward')[0];

            if ('seriesId' in config.page) {
                section.endpoint = '/video/section/' + config.page.section + '/' + config.page.seriesId + '.json?shortUrl=' + config.page.shortUrl;
            } else {
                section.endpoint = '/video/section/' + config.page.section + '.json?shortUrl=' + config.page.shortUrl;
            }
            section.fetch(parentEl).then(function() {
                images.upgrade(parentEl);
            });
        },
        initMostViewedMedia: function() {
            if (config.page.section === 'childrens-books-site' && config.switches.childrensBooksHidePopular) {
                $('.content__secondary-column--media').addClass('u-h');
            } else {
                var mostViewed = new Component();
                mostViewed.endpoint = '/' + config.page.contentType.toLowerCase() + '/most-viewed.json';
                mostViewed.fetch($('.js-video-components-container')[0], 'html');
            }
        },
        displayReleaseMessage: function() {
            var msg = '<p class="site-message__message" id="site-message__message">' +
                    'We\'ve redesigned our video pages to make it easier to find and experience our best video content. We\'d love to hear what you think.' +
                    '</p>' +
                    '<ul class="site-message__actions u-unstyled">' +
                    '<li class="site-message__actions__item">' +
                    '<i class="i i-arrow-white-right"></i>' +
                    '<a href="https://www.surveymonkey.com/s/guardianvideo" target="_blank">Leave feedback</a>' +
                    '</li>' +
                    '<li class="site-message__actions__item">' +
                    '<i class="i i-arrow-white-right"></i>' +
                    '<a href="http://next.theguardian.com/blog/video-redesign/" target="_blank">Find out more</a>' +
                    '</li>' +
                    '</ul>';

            var releaseMessage = new Message('video');

            releaseMessage.show(msg);
        }
    };

    var ready = function () {
        if(config.switches.enhancedMediaPlayer) {
            modules.initPlayer();
        }

        if (config.isMedia) {
            if (config.page.showRelatedContent) {
                modules.initMoreInSection();
            }
            modules.initMostViewedMedia();
        }

        if (config.page.contentType.toLowerCase() === 'video' && detect.getBreakpoint() !== 'mobile') {
            modules.displayReleaseMessage();
        }
    };

    return {
        init: ready
    };
});
