define([
    'bean',
    'raven',
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/_'
], function (
    bean,
    raven,
    $,
    ajax,
    _
    ) {
    function SearchTool($container) {

        var $list = null;

        return {
            init: function () {
                $list = $('.js-search-tool-list', $container);

                this.bindEvents();
            },

            bindEvents: function () {
                bean.on(document.body, 'keyup', $('.js-search-tool-input'), this.getListOfPositions.bind(this));
            },

            getListOfPositions: function (e) {
                if (!e.target.value.match(/\S/)) {
                    this.clear();

                    return;
                }

                var apiKey = '3e74092c580e46319d36f04e68734365';
                var listUrl = 'http://api.accuweather.com/locations/v1/cities/autocomplete?q=' + e.target.value + '&apikey=' + apiKey + '&language=en';

                ajax({
                    url: listUrl,
                    type: 'jsonp',
                    method: 'get',
                    cache: true
                }).then(function (positions) {
                    this.renderList(positions, 3);
                }.bind(this));
            },

            renderList: function (results, numOfResults) {
                var docFragment = document.createDocumentFragment(),
                    len = results.length,
                    toShow = len - numOfResults;

                _(results).initial(toShow).each(function (item) {
                    var li = document.createElement("li");

                    li.innerHTML = '<a role="button" class="weather__results-item">' + item['LocalizedName'] + '</a>';
                    docFragment.appendChild(li);
                });


                this.clear().append(docFragment);
            },

            clear: function () {
                console.log($list);
                return $list.html('');
            }
        };
    }

    return SearchTool;
});
