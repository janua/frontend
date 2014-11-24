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

            $holder = $('.js-weather');

            this.getGeoLocation();
        },

        getGeoLocation: function() {
            navigator.geolocation.getCurrentPosition(this.fetchData);
        },

        fetchData: function (position) {
            var urlLocation = 'http://api.accuweather.com/locations/v1/cities/geoposition/search.json?q='
                    + position.coords.latitude + ', ' + position.coords.longitude + '&apikey=' + apiKey,
                urlWeather = 'http://apidev.accuweather.com/currentconditions/v1/';

            try {
                ajax({
                    url: urlLocation,
                    type: 'jsonp',
                    method: 'get',
                    cache: true
                }).then(function (locationResp) {
                    console.log('test');
                    /*ajax({
                        url: urlWeather + locationResp['Key'] + '.json?apikey=' + apiKey,
                        type: 'jsonp',
                        method: 'get',
                        cache: true
                    }).then(function (weatherResp) {
                        self.views.addToDOM(weatherResp[0], locationResp['AdministrativeArea']['EnglishName']);
                    });*/
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

                $weather.insertAfter($holder);
            }
        }
    };
});

