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
        $holder  = null;

    return {
        init: function () {
            self = this;

            $holder = $('.js-weather');

            this.fetchData();
        },

        fetchData: function () {
            var url = 'http://apidev.accuweather.com/currentconditions/v1/328328.json?apikey=3e74092c580e46319d36f04e68734365';
            try {
                ajax({
                    url: url,
                    type: 'json',
                    method: 'get',
                    crossOrigin: true
                }).then(function (resp) {
                    self.views.addToDOM(resp);
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
            addToDOM: function (data) {
                $weather = $.create(template(weatherTemplate, {
                    location: 'London',
                    icon: data['WeatherIcon'],
                    tempNow: data['Temperature']['Metric']['Value']
                }));


            }
        }
    };
});

