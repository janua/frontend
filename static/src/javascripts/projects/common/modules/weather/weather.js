define([
    'raven',
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/template',
    'text!common/views/components/weather.html'
], function (
    raven,
    $,
    ajax,
    template,
    weatherTemplate
    ) {

    var self     = null,
        $weather = null,
        $holder  = null,
        apiKey   = '3e74092c580e46319d36f04e68734365';

    return {
        init: function () {
            self = this;

            this.getGeoLocation();
        },

        getGeoLocation: function() {
            navigator.geolocation.getCurrentPosition(this.fetchData);
        },

        getLocationData: function(urlLocation) {
            return ajax({
                url: urlLocation,
                type: 'jsonp',
                method: 'get',
                cache: true
            });
        },

        getWeatherData: function(urlWeather, locationData) {
            return ajax({
                url: urlWeather + locationData['Key'] + '.json?apikey=' + apiKey,
                type: 'jsonp',
                method: 'get',
                cache: true
            });
        },

        fetchData: function (position) {
            var urlLocation = 'http://api.accuweather.com/locations/v1/cities/geoposition/search.json?q='
                    + position.coords.latitude + ', ' + position.coords.longitude + '&apikey=' + apiKey,
                urlWeather = 'http://apidev.accuweather.com/currentconditions/v1/';

            try {
                self.getLocationData(urlLocation).then(function (locationResp) {
                    self.getWeatherData(urlWeather, locationResp).then(function (weatherResp) {
                        self.views.addToDOM(weatherResp[0], locationResp['AdministrativeArea']['EnglishName']);
                    });
                });
            } catch (e) {
                raven.captureException(new Error('Error retrieving weather data (' + e.message + ')'), {
                    tags: {
                        feature: 'weather'
                    }
                });
            }
        },

        views: {
            addToDOM: function (weatherData, city) {
                $weather = $.create(template(weatherTemplate, {
                    location: city,
                    icon: weatherData['WeatherIcon'],
                    tempNow: Math.round(weatherData['Temperature']['Metric']['Value'])
                }));

                $holder = $('.js-weather');

                $weather.insertAfter($holder);
            }
        }
    };
});

