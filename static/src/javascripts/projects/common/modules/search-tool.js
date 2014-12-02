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
    function SearchTool(options) {

        var $list      = null,
            $input     = null,
            oldQuery   = '',
            newQuery   = '',
            keyCodeMap = {
                13: "enter",
                38: "up",
                40: "down"
            },
            $container = options.container,
            apiUrl     = options.apiUrl;

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

            hasInputValueChanged: function () {
                return (oldQuery.length !== newQuery.length);
            },

            shouldRequest: function() {
                return $('.active', $list).length === 0;
            },

            getListOfPositions: function (e) {
                if (!e.target.value.match(/\S/)) {
                    this.clear();

                    return;
                }

                if (!this.hasInputValueChanged() ||
                    !this.shouldRequest()) {

                    return;
                }

                oldQuery = newQuery;

                ajax({
                    url: apiUrl + '&q=' + e.target.value,
                    type: 'jsonp',
                    method: 'get',
                    cache: true
                }).then(function (positions) {
                    this.renderList(positions, 3);
                }.bind(this));
            },

            handleKeyEvents: function(e) {
                var key = keyCodeMap[e.which || e.keyCode];

                if (key === 'down') { // down
                    e.preventDefault();
                    this.move(1);
                } else if (key === 'up') { // up
                    e.preventDefault();
                    this.move(-1);
                } else {
                    newQuery = $input.val();
                }
            },

            move: function (increment) {
                var $active = $('.active', $list),
                    id      = parseInt($active.attr('id'), 10),
                    newdId  = -1;

                if (isNaN(id)) {
                    id = -1;
                }

                $active.removeClass('active');

                if (this.getNewId(id + increment) < 0) {
                    $input.val(newQuery);
                } else {
                    $('#' + this.getNewId(id + increment) + 'sti', $list).addClass('active');
                    this.setInputValue();
                }
            },

            getNewId: function(newId) {
                var len = $('li', $list).length;

                newId = newId % len;

                if (newId < -1) {
                    newId = len - 1;
                }

                return newId;
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
