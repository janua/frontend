define([
    'bean',
    'raven',
    'common/utils/_',
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/config',
    'common/utils/template',
    'common/modules/userPrefs',
    'common/modules/ui/toggles',
    'text!common/views/components/weather.html'
], function (
    bean,
    raven,
    _,
    $,
    ajax,
    config,
    template,
    userPrefs,
    Toggles,
    weatherTemplate
    ) {

    var self       = null,
        $weather   = null,
        $holder    = null,
        toggles    = null,
        apiKey     = '3e74092c580e46319d36f04e68734365',
        prefName   = 'weather-location',
        geo        = {
            'London': {
                coords: {
                    latitude: 51.51,
                    longitude: -0.11
                }
            },
            'New York': {
                coords: {
                    latitude: 40.71,
                    longitude: -74.01
                }
            },
            'Sydney': {
                coords: {
                    latitude: -33.86,
                    longitude: 151.21
                }
            }
        },
        getGeoStates = {
            process: "Getting location...",
            error: "Unable to get location...",
            default: "Detect my location"
        };

    return {
        init: function () {
            self = this;

            this.fetchData(this.getUserPrefs());
        },

        getDefaultLocation: function() {
            switch (config.page.edition) {
                case "US": return geo['New York']; break;
                case "AU": return geo['Sydney']; break;
                default: return geo['London'];
            }
        },

        getGeoLocation: function() {
            navigator.geolocation.getCurrentPosition(this.fetchData, this.geoLocationDisabled);
        },

        geoLocationDisabled: function() {
            self.changeLocationOptionText("error");
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

        saveUserLocation: function(position) {
            var toStore = {
                coords: {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude
                }
            };

            userPrefs.set(prefName, toStore);
        },

        /**
         * Check if user has data in local storage.
         * If yes return data from local storage else return default location data.
         *
         * @returns {object} geolocation - lat and long
         */
        getUserPrefs: function () {
            var prefs = userPrefs.get(prefName);

            if (prefs && prefs["coords"]) {
                return prefs;
            }

            return this.getDefaultLocation();
        },

        fetchData: function (position) {
            var urlLocation = 'http://api.accuweather.com/locations/v1/cities/geoposition/search.json?q='
                    + position.coords.latitude + ', ' + position.coords.longitude + '&apikey=' + apiKey,
                urlWeather = 'http://api.accuweather.com/currentconditions/v1/';

            self.saveUserLocation(position);

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

        bindEvents: function() {
            bean.on($('.js-detect-location')[0], 'click', self.detectPosition);
            bean.on($('.js-list-location')[0], 'keyup', self.getListOfPositions);
        },

        unbindEvents: function() {
            bean.off($('.js-detect-location')[0], 'click', self.detectPosition);
        },

        getListOfPositions: function(e) {
            if (!e.target.value.match(/\S/)) {
                $('.js-location-list').html('');
                return;
            }

            var listUrl = 'http://api.accuweather.com/locations/v1/cities/autocomplete?q=' + e.target.value + '&apikey=' + apiKey + '&language=en';

            ajax({
                url: listUrl,
                type: 'jsonp',
                method: 'get',
                cache: true
            }).then(self.showListOfPositions);
        },

        showListOfPositions: function(results) {
            var docFragment = document.createDocumentFragment();

            _(results).initial(7).each(function(item) {
                var li = document.createElement("li");

                li.innerHTML = '<a role="button" class="weather__results-item">' + item['LocalizedName'] + '</a>';
                docFragment.appendChild(li);
            });

            $('.js-location-list').html('').append(docFragment);
        },

        detectPosition: function(e) {
            e.preventDefault();

            self.changeLocationOptionText("process");
            self.getGeoLocation();
        },

        changeLocationOptionText: function(state) {
            $('.js-detect-location').text(getGeoStates[state]);
        },

        views: {
            addToDOM: function (weatherData, city) {
                $weather = $('.weather');

                if ($weather.length > 0) {
                    self.unbindEvents();
                    $weather.remove();
                }

                $weather = $.create(template(weatherTemplate, {
                    location: city,
                    icon: weatherData['WeatherIcon'],
                    tempNow: Math.round(weatherData['Temperature']['Metric']['Value'])
                }));

                $holder = $('.js-weather');

                $weather.insertAfter($holder);

                toggles = new Toggles();
                toggles.init($weather);

                self.bindEvents();
            }
        }
    };
});

