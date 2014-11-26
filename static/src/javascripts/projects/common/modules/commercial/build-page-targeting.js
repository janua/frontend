define([
    'lodash/arrays/compact',
    'lodash/collections/map',
    'lodash/objects/isArray',
    'lodash/objects/merge',
    'lodash/objects/pick',
    'lodash/utilities/identity',
    'common/utils/config',
    'common/utils/cookies',
    'common/utils/detect',
    'common/modules/commercial/keywords',
    'common/modules/commercial/tags/audience-science',
    'common/modules/commercial/tags/audience-science-gateway',
    'common/modules/commercial/tags/criteo',
    'common/modules/commercial/user-ad-targeting',
    'common/modules/experiments/ab'
], function (
    compact,
    map,
    isArray,
    merge,
    pick,
    identity,
    config,
    cookies,
    detect,
    keywords,
    audienceScience,
    audienceScienceGateway,
    criteo,
    userAdTargeting,
    ab
) {

    var formatTarget = function (target) {
            return target ? keywords.format(target).replace(/&/g, 'and').replace(/'/g, '') : null;
        },
        getSeries = function (page) {
            if (page.seriesId) {
                return parseId(page.seriesId);
            }
            var seriesIdFromUrl = /\/series\/(.+)$/.exec(page.pageId);

            return seriesIdFromUrl === null ? '' : seriesIdFromUrl[1];
        },
        parseId = function (id) {
            if (!id) {
                return null;
            }
            return keywords.format(id.split('/').pop());
        },
        parseIds = function (ids) {
            if (!ids) {
                return null;
            }
            return map((ids || '').split(','), function (id) {
                return parseId(id);
            });
        },
        abParam = function () {
            var hchTest = ab.getParticipations().HighCommercialComponent;
            if (hchTest) {
                switch (hchTest.variant) {
                    case 'control':
                        return '1';
                    case 'variant':
                        return '2';
                }
            }
            return '3';
        };

    return function () {
        var page        = config.page,
            contentType = formatTarget(page.contentType),
            pageTargets = merge({
                url:     window.location.pathname,
                edition: page.edition && page.edition.toLowerCase(),
                se:      getSeries(page),
                ct:      contentType,
                pt:      contentType,
                p:       'ng',
                k:       page.keywordIds ? parseIds(page.keywordIds) : parseId(page.pageId),
                su:      page.isSurging,
                bp:      detect.getBreakpoint(),
                a:       audienceScience.getSegments(),
                at:      cookies.get('adtest'),
                gdncrm:  userAdTargeting.getUserSegments(),
                ab:      abParam(),
                co:      parseIds(page.authorIds),
                bl:      parseIds(page.blogIds),
                ms:      formatTarget(page.source),
                tn:      compact([config.page.sponsorshipType].concat(parseIds(page.tones)))
            }, audienceScienceGateway.getSegments(), criteo.getSegments());

        // filter out empty values
        return pick(pageTargets, function (target) {
            if (isArray(target)) {
                return target.length > 0;
            } else {
                return target;
            }
        });
    };

});
