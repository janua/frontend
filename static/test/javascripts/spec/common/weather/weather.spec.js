define([
    'bonzo',
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/template',
    'common/modules/weather/weather',
    'text!common/views/components/weather.html'
], function (
    bonzo,
    $,
    ajax,
    template,
    sut,
    weatherTemplate
    ) {

    ddescribe('Weather component', function() {
        var server;

        beforeEach(function () {
            container = bonzo.create(
                    '<div class="js-weather"></div>'
            )[0];

            server = sinon.fakeServer.create();
        });

        afterEach(function() {
            server.restore();
        });

        it("should initalize", function() {
            spyOn(sut, 'getGeoLocation');

            expect(sut).toEqual(jasmine.any(Object));

            sut.init();

            expect(sut.getGeoLocation).toHaveBeenCalled();
        });

        it("should call fetch data", function() {
            window.navigator.geolocation = {
                "getCurrentPosition": function(success) {}
            };

            spyOn(navigator.geolocation, 'getCurrentPosition');

            sut.init();

            expect(navigator.geolocation.getCurrentPosition).toHaveBeenCalledWith(sut.fetchData);
        });

        it("should get location and weather data", function() {
            spyOn(sut.views, 'addToDOM');

            var mock = {
                coords: {
                    latitude: 50,
                    longitude: 45
                }
            };

            server.respondWith("GET", "/something",
                [200, { "Content-Type": "application/json" },
                    '{ "stuff": "is", "awesome": "in here" }']);

            sut.fetchData(mock);
            server.respond();

            //expect(sut.views.addToDOM).toHaveBeenCalled();
        });
    });
});
