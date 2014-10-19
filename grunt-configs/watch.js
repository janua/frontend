module.exports = function(grunt, options) {
    return {
        js: {
            files: [
                'static/{src,public}/javascripts/**/*.js'
            ],
            tasks: [
                'compile:js',
                'asset_hash'
            ],
            options: {
                spawn: false
            }
        },
        css: {
            files: [
                'static/src/stylesheets/**/*.scss'
            ],
            tasks: [
                'sass:compile',
                'asset_hash'
            ],
            options: {
                spawn: false
            }
        },
        images: {
            files: [
                'static/{src,public}/images/**/*'
            ],
            tasks: [
                'compile:images'
            ]
        },
        flash: {
            files: [
                'static/public/flash/**/*'
            ],
            tasks: [
                'compile:flash'
            ]
        },
        fonts: {
            files: [
                'static/src/stylesheets/components/guss-webfonts/webfonts/**/*'
            ],
            tasks: [
                'compile:fonts'
            ]
        },
        styleguide: {
            files: [
                'static/src/stylesheets/**/*.scss',
                'docs/styleguide/**/*.scss',
                'docs/styleguide_templates/**/*.html'
            ],
            tasks: [
                'compile:css',
                'hologram'
            ],
            options: {
                spawn: false
            }
        },
        clojurescript: {
            files: [
                '/Users/francis/Documents/frontend/admin/assets/**/*.cljs'
            ],
            tasks: [
                'cljs'
            ],
            options: {
                spawn: false
            }
        }
    };
};
