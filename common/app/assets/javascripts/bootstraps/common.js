/* jshint nonew: false */
/* TODO - fix module constructors so we can remove the above jshint override */
define([
    //Commmon libraries
    'common/utils/$',
    'common/utils/mediator',
    'common/utils/deferToLoad',
    'common/utils/ajax',
    'common/modules/userPrefs',
    'common/utils/url',
    //Vendor libraries
    'bonzo',
    'bean',
    'qwery',
    'enhancer',
    'lodash/functions/debounce',
    'fastclick',
    //Modules
    'common/utils/storage',
    'common/utils/detect',
    'common/modules/onward/most-popular-factory',
    'common/modules/onward/related',
    'common/modules/onward/onward-content',
    'common/modules/ui/images',
    'common/modules/navigation/profile',
    'common/modules/navigation/search',
    'common/modules/navigation/navigation',
    'common/modules/ui/tabs',
    'common/modules/ui/toggles',
    'common/modules/ui/dropdowns',
    'common/modules/ui/relativedates',
    'common/modules/analytics/clickstream',
    'common/modules/analytics/omniture',
    'common/modules/analytics/scrollDepth',
    'common/utils/cookies',
    'common/modules/analytics/livestats',
    'common/modules/experiments/ab',
    'common/modules/discussion/comment-count',
    'common/modules/gallery/lightbox',
    'common/modules/onward/history',
    'common/modules/onward/breaking-news',
    'common/modules/ui/message',
    'common/modules/identity/autosignin',
    'common/modules/analytics/foresee-survey',
    'common/modules/onward/geo-most-popular',
    'common/modules/analytics/register',
    'common/modules/onward/tonal',
    'common/modules/identity/api',
    'common/modules/onward/more-tags',
    'common/modules/ui/smartAppBanner',
    'common/modules/ui/fauxBlockLink',
    'common/modules/discussion/loader'
], function (
    $,
    mediator,
    deferToLoadEvent,
    ajax,
    userPrefs,
    Url,

    bonzo,
    bean,
    qwery,
    enhancer,
    debounce,
    FastClick,

    storage,
    detect,
    MostPopularFactory,
    Related,
    Onward,
    images,
    Profile,
    Search,
    Navigation,
    Tabs,
    Toggles,
    Dropdowns,
    RelativeDates,
    Clickstream,
    Omniture,
    ScrollDepth,
    Cookies,
    liveStats,
    ab,
    CommentCount,
    LightboxGallery,
    history,
    breakingNews,
    Message,
    AutoSignin,
    Foresee,
    GeoMostPopular,
    register,
    TonalComponent,
    id,
    MoreTags,
    smartAppBanner,
    FauxBlockLink,
    DiscussionLoader
) {

    var modules = {

        initFastClick: function() {
            new FastClick(document.body);
        },

        upgradeImages: function () {
            images.upgrade();
            images.listen();
        },

        initialiseFauxBlockLink: function(context){
            new FauxBlockLink(context);
        },

        initialiseTopNavItems: function(config){
            var search = new Search(config),
                header = document.getElementById('header');

            if (header) {
                if (config.switches.idProfileNavigation) {
                    var profile = new Profile(header, {
                        url: config.page.idUrl
                    });
                    profile.init();
                }
            }

            search.init(header);
        },

        initialiseNavigation: function (config) {
            Navigation.init(config);
        },

        transcludeRelated: function (config, context) {
            var r = new Related();
            r.renderRelatedComponent(config, context);
        },

        transcludePopular: function (config) {
            new MostPopularFactory(config);
        },

        transcludeOnwardContent: function(config, context){
            if ('seriesId' in config.page) {
                new Onward(config, qwery('.js-onward', context));
            } else if (config.page.tones !== '') {
                $('.js-onward', context).each(function(c) {
                    new TonalComponent(config, c).fetch(c, 'html');
                });
            }
        },

        showTabs: function() {
            var tabs = new Tabs();
            mediator.on('modules:popular:loaded', function(el) {
                tabs.init(el);
            });
        },

        showToggles: function() {
            var toggles = new Toggles();
            toggles.init(document);
            mediator.on('page:common:ready', function() {
                toggles.reset();
                Dropdowns.init();
            });
        },

        showRelativeDates: function () {
            var dates = RelativeDates;
            mediator.on('page:common:ready', function(config, context) {
                dates.init(context);
            });
            mediator.on('fragment:ready:dates', function(el) {
                dates.init(el);
            });
        },

        initClickstream: function () {
            new Clickstream({filter: ['a', 'button']});
        },

        transcludeCommentCounts: function () {
            mediator.on('page:common:ready', function(config, context) {
                CommentCount.init(context);
            });
        },

        initLightboxGalleries: function () {
            var thisPageId;
            mediator.on('page:common:ready', function(config, context) {
                var galleries = new LightboxGallery(config, context);
                thisPageId = config.page.pageId;
                galleries.init();
            });

            // Register as a page view if invoked from elsewhere than its gallery page (like a trailblock)
            mediator.on('module:lightbox-gallery:loaded', function(config, context) {
                if (thisPageId !== config.page.pageId) {
                    mediator.emit('page:common:deferred:loaded', config, context);
                }
            });
        },

        initRightHandComponent: function(config) {
            if(config.page.contentType === 'Article' &&
                detect.getBreakpoint() !== 'mobile' &&
                parseInt(config.page.wordCount, 10) > 500) {
                new GeoMostPopular({});
            }
        },

        logLiveStats: function (config) {
            liveStats.log(config);
        },

        loadAnalytics: function (config) {
            var omniture = new Omniture();

            omniture.go(config);

            if (config.switches.ophan && !config.page.isSSL) {
                require('ophan/ng', function (ophan) {
                    ophan.record({'ab': ab.getParticipations()});

                    if(config.switches.scrollDepth) {
                        mediator.on('scrolldepth:data', ophan.record);

                        new ScrollDepth({
                            isContent: /Article|LiveBlog/.test(config.page.contentType)
                        });
                    }
                });
            }
        },

        cleanupCookies: function() {
            Cookies.cleanUp(['mmcore.pd', 'mmcore.srv', 'mmid', 'GU_ABFACIA', 'GU_FACIA', 'GU_ALPHA']);
            Cookies.cleanUpDuplicates(['GU_VIEW']);
        },

        // opt-in to the responsive alpha
        optIn: function () {
            var countMeIn = /#countmein/.test(window.location.hash);
            if (countMeIn) {
                Cookies.add('GU_VIEW', 'responsive', 365);
            }
        },

        // display a flash message to devices over 600px who don't have the mobile cookie
        displayReleaseMessage: function (config) {

            var path = (document.location.pathname) ? document.location.pathname : '/',
                exitLink = '/preference/platform/classic?page=' + encodeURIComponent(path + '?view=classic'),
                msg = '<p class="site-message__message" id="site-message__message">' +
                            'You’re viewing a beta release of the Guardian’s responsive website.' +
                            ' We’d love to hear your <a href="https://www.surveymonkey.com/s/theguardian-beta-feedback" data-link-name="feedback">feedback</a>' +
                      '</p>' +
                      '<ul class="site-message__actions u-unstyled">' +
                           '<li class="site-message__actions__item">' +
                               '<i class="i i-back"></i>' +
                                   '<a class="js-main-site-link" rel="nofollow" href="' + exitLink + '"' +
                                       'data-link-name="opt-out">Opt-out and return to our current site </a>' +
                           '</li>' +
                      '</ul>';

            var releaseMessage = new Message('alpha');

            // Do not show the release message on -sp- based paths.
            var spRegExp = new RegExp('.*-sp-.*');

            if (config.switches.releaseMessage && (detect.getBreakpoint() !== 'mobile') && !spRegExp.test(path) && config.page.contentType !== 'Video' && config.page.hasClassicVersion) {
                // force the visitor in to the alpha release for subsequent visits
                Cookies.add('GU_VIEW', 'responsive', 365);
                releaseMessage.show(msg);
            }
        },

        displayBreakingNews: function (config) {
            if (config.switches.breakingNews) {
                breakingNews(config);
            }
        },

        unshackleParagraphs: function (config, context) {
            if (userPrefs.isOff('para-indents')) {
                $('.paragraph-spacing--indents', context).removeClass('paragraph-spacing--indents');
            }
        },

        logReadingHistory : function() {
            mediator.on('page:common:ready', function(config) {
                if(config.page.contentType !== 'Network Front') {
                    history.log({
                        id: '/' + config.page.pageId,
                        meta: {
                            section: config.page.section,
                            keywords: config.page.keywordIds && (config.page.keywordIds + '').split(',').slice(0, 5)
                        }
                    });
                }
            });
        },

        initAutoSignin : function() {
           mediator.on('page:common:ready', function(config) {
                if (config.switches && config.switches.facebookAutosignin && detect.getBreakpoint() !== 'mobile') {
                    new AutoSignin(config).init();
                }
            });
        },

        windowEventListeners: function() {
            var events = {
                    resize: 'window:resize',
                    orientationchange: 'window:orientationchange',
                    scroll: 'window:scroll'
                },
                emitEvent = function(eventName) {
                    return function(e) {
                        mediator.emit(eventName, e);
                    };
                };
            for (var event in events) {
                bean.on(window, event, debounce(emitEvent(events[event]), 200));
            }
        },

        checkIframe: function() {
            if (window.self !== window.top) {
                $('html').addClass('iframed');
            }
        },

        runForseeSurvey: function(config) {
            if(config.switches.foresee) {
                Foresee.load();
            }
        },

        augmentInteractive: function() {
            mediator.on('page:common:ready', function(config, context) {
                if (/Article|Interactive|LiveBlog/.test(config.page.contentType)) {
                    $('figure.interactive').each(function (el) {
                        enhancer.render(el, context, config, mediator);
                    });
                }
            });
        },

        startRegister: function(config) {
            if (!config.page.isSSL) {
                register.initialise(config);
            }
        },

        repositionComments: function() {
            mediator.on('page:common:ready', function() {
                if(!id.isUserLoggedIn()) {
                    $('.js-comments').appendTo(qwery('.js-repositioned-comments'));
                }
            });
        },

        showMoreTagsLink: function() {
            new MoreTags().init();
        },

        showSmartBanner: function(config) {
            if(config.switches.smartBanner) {
                smartAppBanner.init();
            }
        },

        initDiscussion: function() {
            mediator.on('page:common:ready', function(config, context) {
                if (config.page.commentable && config.switches.discussion) {
                    var discussionLoader = new DiscussionLoader(context, mediator, { 'switches': config.switches });
                    discussionLoader.attachTo($('.discussion')[0]);
                }
            });
        },

        testCookie: function() {
            var queryParams = Url.getUrlVars();
            if (queryParams.test) {
                Cookies.addSessionCookie('GU_TEST', encodeURIComponent(queryParams.test));
            }
        }
    };

    var deferrable = function (config, context) {
        var self = this;
        deferToLoadEvent(function() {
            if (!self.initialisedDeferred) {
                self.initialisedDeferred = true;
                modules.logLiveStats(config);
                modules.loadAnalytics(config, context);
                modules.cleanupCookies(context);
                modules.transcludePopular(config);
                modules.transcludeRelated(config, context);
                modules.transcludeOnwardContent(config, context);
                modules.initRightHandComponent(config, context);
            }
            mediator.emit('page:common:deferred:loaded', config, context);
        });
    };

    var ready = function (config, context) {
        if (!this.initialised) {
            this.initialised = true;
            modules.initFastClick();
            modules.testCookie();
            modules.windowEventListeners();
            modules.initialiseFauxBlockLink(context);
            modules.checkIframe();
            modules.upgradeImages();
            modules.showTabs();
            modules.initialiseTopNavItems(config);
            modules.initialiseNavigation(config);
            modules.displayBreakingNews(config);
            modules.showToggles();
            modules.showRelativeDates();
            modules.initClickstream();
            modules.transcludeCommentCounts();
            modules.initLightboxGalleries();
            modules.optIn();
            modules.displayReleaseMessage(config);
            modules.logReadingHistory();
            modules.unshackleParagraphs(config, context);
            modules.initAutoSignin(config);
            modules.augmentInteractive();
            modules.runForseeSurvey(config);
            modules.startRegister(config);
            modules.repositionComments();
            modules.showMoreTagsLink();
            modules.showSmartBanner(config);
            modules.initDiscussion();
        }
        mediator.emit('page:common:ready', config, context);
    };

    var init = function (config, context) {
        ready(config, context);
        deferrable(config, context);
    };

    return {
        init: init
    };
});

