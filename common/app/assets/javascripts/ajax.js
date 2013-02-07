define(["reqwest"], function (reqwest) {

    function makeAbsolute () {
        throw new Error("AJAX has not been initialised yet");
    };

    return {
        reqwest: reqwest,

        init: function (absoluteUrl) {
            absoluteUrl = absoluteUrl || "";
            makeAbsolute = function (url) {
                return absoluteUrl + url;
            };
        },

        relative: function(params) {
            this.reqwest(params);
        },

        apiEndpoint: function(params) {
            params.url = makeAbsolute(params.url);
            return this.reqwest(params);
        }
    }

});