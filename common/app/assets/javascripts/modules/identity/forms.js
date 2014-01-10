define([
    'bean',
    'bonzo',
    'common/modules/identity/api'
], function (
    bean,
    bonzo,
    IdApi
) {

    function forgottenEmail(config, context) {
        var form = context.querySelector('.js-reset-form');
        if (form) {
            var hashEmail = window.location.hash.match('email=([^&#]*)');
            if (hashEmail) {
                var input = form.querySelector('.js-reset-email');
                input.value = hashEmail[1];
            }
        }
    }

    function forgottenPassword(config, context) {
        var form = context.querySelector('.js-signin-form');
        if (form) {
            var email = form.querySelector('.js-signin-email'),
                link = form.querySelector('.js-forgotten-password'),
                href = link.getAttribute('href');

            bean.add(link, 'click', function(e) {
                var emailAddress = email.value;
                if (emailAddress !== '') {
                    link.setAttribute('href', href + '#email=' + emailAddress);
                }
            });
        }
    }

    function passwordToggle(config, context) {
        var form = context.querySelector('.js-register-form');
        if (form) {
            var password = form.querySelector('.js-register-password'),
                toggleClass = 'js-toggle-password',
                toggleTmpl = '<div class="form-field__note form-field__note--right mobile-only">' +
                                '<a href="#toggle-password" class="' + toggleClass + '" data-password-label="Show password"' +
                                ' data-text-label="Hide password" data-link-name="Toggle password field">Show password</a>' +
                             '</div>',
                $toggle = bonzo(bonzo.create(toggleTmpl)).insertBefore(password);

            $toggle.previous().addClass('form-field__note--left');
            bean.add($toggle[0], '.' + toggleClass, 'click', function(e) {
                e.preventDefault();
                var link = e.target,
                    inputType = password.getAttribute('type') === 'password' ? 'text' : 'password',
                    label = link.getAttribute('data-' + inputType + '-label');
                password.setAttribute('type', inputType);
                bonzo(link).text(label);
            });
        }
    }

    function resendValidationEmail(config, context) {
        var form = context.querySelector('.js-profile-form');
        if (form) {
            var resendButton = form.querySelector('.js-resend-validation-email'),
                $resendButton = bonzo(resendButton);
            
            bean.on(resendButton, 'click', function (event) {
                event.preventDefault();
                
                $resendButton.css('width', resendButton.offsetWidth);
                resendButton.innerHTML = "Loading...";
                
                IdApi.resendValidationEmail()
                    .then(function (response) {
                        $resendButton.replaceWith("<p>Sent. Please check your email and follow the link.</p>");
                    }).fail(function (error) {
                        resendButton.innerHTML = "Resend my verification email";
                    });
            });
        }
    }

    return {
        forgottenEmail: forgottenEmail,
        forgottenPassword: forgottenPassword,
        passwordToggle: passwordToggle,
        resendValidationEmail: resendValidationEmail
    };
});
