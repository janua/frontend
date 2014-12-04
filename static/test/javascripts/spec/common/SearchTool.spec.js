define([
    'bean',
    'bonzo',
    'common/utils/$',
    'common/modules/search-tool'
], function (
    bean,
    bonzo,
    $,
    SearchTool
    ) {

    ddescribe('Search tool', function () {
        var container,
            sut;

        beforeEach(function () {
            container = bonzo.create(
                '<div class="search-tool js-search-tool">' +
                    '<div class="search-tool__form u-cf">' +
                        '<input class="search-tool__input js-search-tool-input" type="text" data-link-name="weather-list-location" />' +
                        '<button class="search-tool__btn"><i class="i i-search-white-36"></i></button>' +
                    '</div>' +
                    '<ul class="u-unstyled js-search-tool-list"></ul>' +
                '</div>'
            )[0];

            $('body').append(container);

            sut = new SearchTool({
                container: $('.js-search-tool'),
                apiUrl: 'http://testapiurl'
            });
        });

        afterEach(function() {
            $('body').html("");
            container = null;
        });

        it("should be defined", function() {
            expect(sut).toEqual(jasmine.any(Object));
        });

        it("should bind events after initialization", function() {
            spyOn(sut, "bindEvents");

            sut.init();

            expect(sut.bindEvents).toHaveBeenCalled();
        });

        it("should respond to keydown event", function() {
            var stubEvent = {
                keyCode: 38,
                preventDefault: function() {}
            };

            spyOn(sut, "move");
            spyOn(sut, "pushData");
            spyOn(sut, "getListOfResults");

            // Test for up key
            sut.handleKeyEvents(stubEvent);
            expect(sut.move).toHaveBeenCalledWith(-1);

            // Test for down key
            stubEvent.keyCode = 40;
            sut.handleKeyEvents(stubEvent);
            expect(sut.move).toHaveBeenCalledWith(1);

            // Test for down key
            stubEvent.keyCode = 13;
            sut.handleKeyEvents(stubEvent);
            expect(sut.pushData).toHaveBeenCalled();

            // Test for any other key
            stubEvent.keyCode = 22;
            sut.handleKeyEvents(stubEvent);
            expect(sut.getListOfResults).toHaveBeenCalledWith(stubEvent);
        });

        it("should push data after click on list item", function() {
            spyOn(sut, "pushData");

            $(".js-search-tool-list").html("<li><a></a></li>");

            sut.init();

            bean.fire($(".js-search-tool-list a")[0], "click");

            expect(sut.pushData).toHaveBeenCalled();
        });

        it("should return new ID", function() {
            $(".js-search-tool-list").html("<li></li><li></li><li></li><li></li>");

            expect(sut.getNewId(0)).toEqual(0);
            expect(sut.getNewId(1)).toEqual(1);
            expect(sut.getNewId(2)).toEqual(2);
            expect(sut.getNewId(3)).toEqual(3);
            expect(sut.getNewId(4)).toEqual(-1);
            expect(sut.getNewId(-1)).toEqual(-1);
        });

        it("should not call for results if data haven't change", function() {
            var stubEvent = {
                keyCode: 38,
                preventDefault: function() {},
                target: {
                    value: "test"
                }
            };

            spyOn(sut, "fetchData");
            spyOn(sut, "hasInputValueChanged").and.returnValue(false);

            sut.getListOfResults(stubEvent);

            expect(sut.fetchData).not.toHaveBeenCalled();
        });

        it("should not call for results if data haven't change", function() {
            var stubEvent = {
                keyCode: 38,
                preventDefault: function() {},
                target: {
                    value: "test"
                }
            };

            spyOn(sut, "fetchData");
            spyOn(sut, "hasInputValueChanged").and.returnValue(false);

            sut.getListOfResults(stubEvent);

            expect(sut.fetchData).not.toHaveBeenCalled();
        });
    });
});
