define([
    'lodash/objects/defaults',
    'common/utils/config',
    'common/utils/cookies',
    'common/utils/url'
], function (
    defaults,
    config,
    cookies,
    urlUtils
    ) {

    var netId = '1476',
        cookieName = 'cto2_guardian',
        criteoUrl = '//rtax.criteo.com/delivery/rta/rta.js',
        varName = 'crtg_content';

    function getSegments(c) {
        c = c || config;
        var cookieValue = decodeURIComponent(cookies.get(cookieName));
        return (c.switches.criteo && cookieValue) ? urlUtils.getUrlVars({query: cookieValue}) : {};
    }

    function load(c) {
        c = c || config;
        if (c.switches.criteo) {
            var query = urlUtils.constructQuery({
                netid: netId,
                cookieName: cookieName,
                rnd: Math.floor(Math.random() * 99999999999),
                varName: varName
            });
            return require(['js!' + criteoUrl + '?' + query + '!exports=' + varName]);
        }
    }

    return {
        load : load,
        getSegments: getSegments
    };

});
