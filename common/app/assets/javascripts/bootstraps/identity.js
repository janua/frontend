define([
	"common/$",
    "common/modules/identity/forms",
    "common/modules/identity/formstack",
    "common/modules/identity/formstack-iframe",
    "common/modules/identity/password-strength",
    "common/modules/identity/api",
    "common/modules/adverts/userAdTargeting",
    "common/modules/discussion/user-avatars",
    "common/utils/mediator"
], function(
    $,
    Identity,
    Formstack,
    FormstackIframe,
    PasswordStrength,
    Id,
    UserAdTargeting,
    UserAvatars,
    mediator
) {

    var modules = {
        idInit: function (config) {
            Id.init(config);
        },
        initFormstack: function () {
            mediator.on('page:identity:ready', function(config, context) {
                var attr = 'data-formstack-id';
                $('[' + attr + ']').each(function(el) {
                    var id = el.getAttribute(attr);
                    new Formstack(el, id, context, config).init();
                });
                $('.js-formstack-iframe').each(function(el) {
                    new FormstackIframe(el, context, config).init();
                });
            });
        },
        forgottenEmail: function () {
            mediator.on('page:identity:ready', function(config, context) {
                Identity.forgottenEmail(config, context);
            });
        },
        forgottenPassword: function () {
            mediator.on('page:identity:ready', function(config, context) {
                Identity.forgottenPassword(config, context);
            });
        },
        passwordStrength: function () {
            mediator.on('page:identity:ready', function(config, context) {
                $('.js-password-strength').each(function(el) {
                    new PasswordStrength(el, context, config).init();
                });
            });
        },
        passwordToggle: function () {
            mediator.on('page:identity:ready', function(config, context) {
                Identity.passwordToggle(config, context);
            });
        },
        userAdTargeting : function () {
            mediator.on('page:identity:ready', function(config, context) {
                UserAdTargeting.requestUserSegmentsFromId();
            });
        },
        userAvatars: function() {
            mediator.on('page:identity:ready', function(config, context) {
                UserAvatars.init();
            });
        },
        resendValidationEmail: function () {
            mediator.on('page:identity:ready', function(config, context) {
                Identity.resendValidationEmail(config, context);
            });
        }
    };

    var ready = function (config, context) {
        if (!this.initialised) {
            this.initialised = true;
            modules.idInit(config);
            modules.initFormstack();
            modules.forgottenEmail();
            modules.forgottenPassword();
            modules.passwordStrength();
            modules.passwordToggle();
            modules.userAdTargeting();
            modules.userAvatars();
            modules.resendValidationEmail();
        }
        mediator.emit("page:identity:ready", config, context);
    };

    return {
        init: ready
    };

});
