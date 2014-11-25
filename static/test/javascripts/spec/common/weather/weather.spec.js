define([
    'bonzo',
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/template',
    'common/modules/weather/weather'
], function (
    bonzo,
    $,
    ajax,
    template,
    sut
    ) {

    ddescribe('Weather component', function() {
        var container,
            $weather;

        beforeEach(function () {
            container = bonzo.create(
                    '<div><div class="js-weather"></div></div>'
            )[0];

            $('body').append(container);
        });

        afterEach(function() {
            $('body').html();
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

        it("should add weather component to the DOM", function() {
            var mockWeatherData = {
                WeatherIcon: 3,
                Temperature: {
                    Metric: {
                        Value: 9.1
                    }
                }
            };

            var mockCity = 'London';

            sut.views.addToDOM(mockWeatherData, mockCity);

            $weather = $('.weather');

            expect($(".weather__city", $weather).text()).toEqual('London');
            expect($(".weather__temp", $weather).text()).toEqual('9°');
            expect($(".weather__icon", $weather).hasClass('i-weather-' + mockWeatherData["WeatherIcon"])).toBeTruthy();
        });
    });
});
