define([
    'common/$',
    'qwery',
    'bonzo',
    'common/utils/ajax',
    'common/modules/userPrefs',
    'common/utils/detect',
    'common/modules/adverts/document-write',
    'common/modules/adverts/documentwriteslot',
    'common/modules/adverts/dimensionMap',
    'common/modules/adverts/userAdTargeting'
],
function (
    $,
    qwery,
    bonzo,
    ajax,

    userPrefs,
    detect,
    documentWrite,
    DocumentWriteSlot,
    dimensionMap,
    userAdTargeting
) {

    var currConfig,
        currContext,
        slots,
        contexts = {};

    function init(config, context) {
        var id = context.id;

        if(id) {

            contexts[id] = context;
            currConfig  = config;
            currContext = context;
            slots = [];

            var size = (window.innerWidth > 810) ? 'median' : 'base';

            // Run through slots and create documentWrite for each.
            // Other ad types such as iframes and custom can be plugged in here later

            for (var c in contexts) {
                var els = contexts[c].querySelectorAll('.ad-slot');
                for(var i = 0, l = els.length; i < l; i += 1) {
                    var container = els[i].querySelector('.ad-container'),
                        name,
                        slot;
                    // Empty all ads in the dom
                    container.innerHTML = '';
                    // Load the currContext ads only
                    if (contexts[c] === currContext ) {
                        name = els[i].getAttribute('data-' + size);
                        slot = new DocumentWriteSlot(name, container);
                        slot.setDimensions(dimensionMap[name]);
                        slots.push(slot);
                    }
                }
            }
        }

        //Make the request to ad server
        documentWrite.load({
            config: currConfig,
            slots: slots,
            userSegments : userAdTargeting.getUserSegments()
        });
    }

    function load() {
        //Run through adslots and check if they are on screen. Load if so.
        for (var i = 0, j = slots.length; i<j; ++i) {
            //Add && isOnScreen(slots[i].el) to conditional below to trigger lazy loading
            if (!slots[i].loaded && slots[i].el.innerHTML === '') {
                slots[i].render();
            }
        }

        //This is a horrible hack to hide slot if no creative is returned from oas
        //Check existence of empty tracking pixel
        if(currConfig.page.pageId === "") {
            var middleSlot = currContext.querySelector('.ad-slot-middle-banner-ad');

            if(middleSlot && middleSlot.innerHTML.indexOf("x55/default/empty.gif")  !== -1) {
                bonzo(middleSlot).hide();
            }
        }
    }

    function destroy() {
        for (var i = 0, j = slots.length; i<j; ++i) {
            slots[i].el.innerHTML = '';
            slots[i].loaded = false;
        }
    }

    function reload() {
        destroy();
        init(currConfig, currContext);
    }

    function isOnScreen(el) {
        return (
            el.offsetTop < (window.innerHeight + window.pageYOffset) &&
            (el.offsetTop + el.offsetHeight) > window.pageYOffset
        );
    }

    function hide() {
        $('.ad-slot').addClass('is-invisible');
    }

    //Temporary middle slot needs better implementation in the future
    function generateMiddleSlot() {
        var slot,
            prependTo;

        if(currConfig.page.pageId === "") {
            prependTo = currContext.querySelector('.front-trailblock-commentisfree li');

            if(!bonzo(prependTo).hasClass('middleslot-loaded')) {
                bonzo(prependTo).addClass('middleslot-loaded');

                slot = '<div class="ad-slot-middle-banner-ad ad-slot" data-link-name="ad slot middle-banner-ad"';
                slot+= ' data-base="x55" data-median="x55"><div class="ad-container"></div></div>';

                bonzo(prependTo).after(slot);
            }
        }
    }

    return {
        hide: hide,
        init: init,
        load: load,
        reload: reload,
        isOnScreen: isOnScreen
    };

});
