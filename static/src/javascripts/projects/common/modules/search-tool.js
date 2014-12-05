define([
    'bean',
    'raven',
    'common/utils/$',
    'common/utils/_',
    'common/utils/ajax',
    'common/utils/mediator'
], function (
    bean,
    raven,
    $,
    _,
    ajax,
    mediator
    ) {
    function SearchTool(options) {

        var $list      = null,
            $input     = null,
            oldQuery   = '',
            newQuery   = '',
            keyCodeMap = {
                13: 'enter',
                38: 'up',
                40: 'down'
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
                bean.on(document.body, 'keydown', this.handleKeyEvents.bind(this));
                bean.on(document.body, 'click', $list, this.handleClick.bind(this));
            },

            hasInputValueChanged: function () {
                return (oldQuery.length !== newQuery.length);
            },

            handleClick: function (e) {
                e.preventDefault();

                $input.val(e.target.textContent);

                this.pushData();
            },

            pushData: function () {
                mediator.emit('autocomplete:fetch', [$input.val()]);

                // Clear all after timeout because of the ophan tracking we can't remove everything straight away
                setTimeout(this.destroy.bind(this), 50);
            },

            getListOfResults: function (e) {
                if (!e.target.value.match(/\S/)) {
                    this.clear();

                    return;
                }

                newQuery = e.target.value;

                if (!this.hasInputValueChanged()) {
                    return;
                }

                this.fetchData();
            },

            fetchData: function () {
                return ajax({
                    url: apiUrl + '&q=' + newQuery,
                    type: 'json',
                    crossOrigin: true
                }).then(function (positions) {
                    this.renderList(positions, 3);
                    oldQuery = newQuery;
                }.bind(this));
            },

            handleKeyEvents: function (e) {
                var key = keyCodeMap[e.which || e.keyCode];

                if (key === 'down') { // down
                    e.preventDefault();
                    this.move(1);
                } else if (key === 'up') { // up
                    e.preventDefault();
                    this.move(-1);
                } else if (key === 'enter') { // enter
                    this.pushData();
                } else {
                    this.getListOfResults(e);
                }
            },

            move: function (increment) {
                var $active = $('.active', $list),
                    id      = parseInt($active.attr('id'), 10);

                if (isNaN(id)) {
                    id = -1;
                }

                $active.removeClass('active');

                // When outside of the list show latest query
                if (this.getNewId(id + increment) < 0) {
                    $input.val(oldQuery);

                // When looping inside of the list show list item
                } else {
                    $('#' + this.getNewId(id + increment) + 'sti', $list).addClass('active');
                    this.setInputValue();
                }
            },

            getNewId: function (id) {
                var len   = $('li', $list).length,
                    newId = 0;

                newId = id % len;

                // Make sure that we can hit saved input option which has position -1
                if (newId < -1) {
                    newId = len - 1;
                } else if (id === len) {
                    newId = -1;
                }

                return newId;
            },

            setInputValue: function () {
                var $active = $('.active', $list);

                $input.val($active.text());
            },

            renderList: function (results, numOfResults) {
                var docFragment = document.createDocumentFragment(),
                    len = results.length,
                    toShow = len - numOfResults;

                _(results).initial(toShow).each(function (item, index) {
                    var li = document.createElement('li');

                    li.innerHTML = '<a role="button" id="' + index + 'sti" class="search-tool__item" data-link-name="search-tool">' + item.LocalizedName + ' (' + item.Country.LocalizedName + ')</a>';
                    docFragment.appendChild(li);
                });

                this.clear().append(docFragment);
            },

            clear: function () {
                return $list.html('');
            },

            destroy: function () {
                this.clear();
                $input.val('');
            }
        };
    }

    return SearchTool;
});
