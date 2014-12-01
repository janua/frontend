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

        var $list    = null,
            $input   = null,
            oldQuery = '';

        return {
            init: function () {
                $list  = $('.js-search-tool-list', $container);
                $input = $('.js-search-tool-input', $container);

                this.bindEvents();
            },

            bindEvents: function () {
                bean.on(document.body, 'keyup', $input, this.getListOfPositions.bind(this));
                bean.on(document.body, 'keydown', this.handleKeyEvents.bind(this));
            },

            hasInputValueChanged: function (inputValue) {
                return (oldQuery.length !== inputValue.length);
            },

            getListOfPositions: function (e) {
                if (!e.target.value.match(/\S/)) {
                    this.clear();

                    return;
                }

                if (!this.hasInputValueChanged(e.target.value)) {
                    return;
                }

                oldQuery = e.target.value;

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

            handleKeyEvents: function(e) {
                if ($('.active', $list).length > 0) {
                    if (e.keyCode === 40) { // down
                        this.move('next');
                    } else if (e.keyCode === 38) { // up
                        this.move('previous');
                    }
                } else {
                    $('a', $list).first().toggleClass('active');
                }
            },

            move: function (route) {
                var $active = $('.active', $list);
                    id      = parseInt($active.attr('id'), 10);

                if (route === 'next') {
                    $active.removeClass('active')
                    $('#' + (id + 1) + 'sti', $list).addClass('active');
                } else {
                    $active.removeClass('active');
                    $('#' + (id - 1) + 'sti', $list).addClass('active');
                }
            },

            setInputValue: function() {
                var $active = $('.active', $list);

                $input.val($active.text());
            },

            renderList: function (results, numOfResults) {
                var docFragment = document.createDocumentFragment(),
                    len = results.length,
                    toShow = len - numOfResults;

                _(results).initial(toShow).each(function (item, index) {
                    var li = document.createElement("li");

                    console.log(item);

                    li.innerHTML = '<a role="button" id="' + index + 'sti" class="search-tool__item">' + item['LocalizedName'] + ' (' + item['Country']['LocalizedName'] + ')</a>';
                    docFragment.appendChild(li);
                });


                this.clear().append(docFragment);
            },

            clear: function () {
                return $list.html('');
            }
        };
    }

    return SearchTool;
});
