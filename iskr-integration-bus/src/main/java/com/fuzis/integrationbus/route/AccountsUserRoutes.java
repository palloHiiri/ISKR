package com.fuzis.integrationbus.route;

import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.exception.NoRequiredHeader;
import com.fuzis.integrationbus.exception.ServiceFall;
import com.fuzis.integrationbus.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AccountsUserRoutes extends RouteBuilder {

    private final ChangeSSOUserDataProcessor changeSSOUserDataProcessor;

    private final ChangeSSOUserPasswordProcessor changeSSOUserPasswordProcessor;

    private final SearchUserProcessor searchUserProcessor;

    private final ChangeSSOUserAccountStateProcessor  changeSSOUserAccountStateProcessor;

    private final GetSSOUserAccountRoleProcessor getSSOUserAccountRoleProcessor;

    private final LoginProcessor loginProcessor;
    private final CreateSSOUserAccountProcessor createSSOUserAccountProcessor;

    @Autowired
    public AccountsUserRoutes(ChangeSSOUserDataProcessor changeSSOUserDataProcessor,
                              SearchUserProcessor searchUserProcessor,
                              ChangeSSOUserPasswordProcessor changeSSOUserPasswordProcessor,
                              ChangeSSOUserAccountStateProcessor changeSSOUserAccountStateProcessor,
                              GetSSOUserAccountRoleProcessor getSSOUserAccountRoleProcessor,
                              LoginProcessor loginProcessor,
                              CreateSSOUserAccountProcessor createSSOUserAccountProcessor) {
        this.changeSSOUserDataProcessor = changeSSOUserDataProcessor;
        this.searchUserProcessor = searchUserProcessor;
        this.changeSSOUserPasswordProcessor = changeSSOUserPasswordProcessor;
        this.changeSSOUserAccountStateProcessor = changeSSOUserAccountStateProcessor;
        this.getSSOUserAccountRoleProcessor = getSSOUserAccountRoleProcessor;
        this.loginProcessor = loginProcessor;
        this.createSSOUserAccountProcessor = createSSOUserAccountProcessor;
    }

    @Override
    public void configure() {
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(0)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        from("platform-http:/oapi/v1/accounts/login?httpMethodRestrict=POST")
                .routeId("login-user-post-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .process(loginProcessor)
                .to("direct:finalize-request");

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=GET")
                .routeId("accounts-user-get-route")
                .setHeader("X-Roles-Required", constant(""))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/profile?httpMethodRestrict=GET")
                .routeId("accounts-profile-get-route")
                .setHeader("X-Headers-Required", constant("X-User-Change-ID"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-Change-ID}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/verify-email?httpMethodRestrict=POST")
                .routeId("accounts-verify-email-route")
                .setHeader("X-Roles-Required", constant("profile-watch"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Integration"))
                .setHeader("X-Service-Request", simple("oapi-inner/v1/accounts/verify-email"))
                .setBody(constant(""))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi-inner/v1/accounts/verify-email?httpMethodRestrict=POST")
                .routeId("accounts-inner-verify-email-route")
                .setHeader("X-Headers-Required", constant("X-User-ID"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-No-Meta", constant(true))
                .setHeader("X-Service-Request", simple("api/v1/accounts/token"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("type=verify_email_token&userId=${header.X-User-ID}"))
                .to("direct:sd-call-finalize");
        
        from("platform-http:/oapi/v1/accounts/redeem-token?httpMethodRestrict=POST")
                .routeId("accounts-redeem-token-route")
                .setHeader("X-Headers-Required", constant("Token"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Integration"))
                .setHeader("X-Service-Request", simple("oapi-inner/v1/accounts/redeem-token"))
                .setBody(constant(""))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi-inner/v1/accounts/redeem-token?httpMethodRestrict=POST")
                .routeId("accounts-inner-redeem-token-route")
                .setHeader("X-Headers-Required", constant("Token"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-No-Meta", constant(true))
                .setHeader("X-Service-Request", simple("api/v1/accounts/token/redeem"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("token=${header.Token}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi-inner/v1/accounts/verify-email-sso?httpMethodRestrict=POST")
                .routeId("accounts-inner-verify-email-sso-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("Email-Verified, X-User-ID"))
                .setHeader("X-Headers-Forbidden", constant("New-Nickname, New-Username"))
                .to("direct:check-params")
                .process(searchUserProcessor)
                .process(changeSSOUserDataProcessor)
                .end();

        from("platform-http:/oapi/v1/accounts/reset-password?httpMethodRestrict=POST")
                .routeId("accounts-reset-password-route")
                .setHeader("X-Headers-Required", constant("Login"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/token/reset-password"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                    .setBody(simple("login=${header.Login}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/reset-password-confirm?httpMethodRestrict=POST")
                .routeId("accounts-reset-password-confirm-route")
                .setHeader("X-Headers-Required", constant("Token,Password"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/token/reset-password-confirm"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("token=${header.Token}&password=${header.Password}"))
                .to("direct:sd-call")
                .to("direct:finalize-request");

        from("platform-http:/oapi-inner/v1/accounts/change-password-sso?httpMethodRestrict=POST")
                .routeId("accounts-inner-change-password-sso-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Password, X-User-ID"))
                .to("direct:check-params")
                .process(searchUserProcessor)
                .process(changeSSOUserPasswordProcessor)
                .end();

        from("platform-http:/oapi-inner/v1/accounts/update-user-sso?httpMethodRestrict=POST")
                .routeId("accounts-inner-update-user-sso-route")
                    .onException(NoRequiredHeader.class)
                    .handled(true)
                .to("direct:bad-request-error-handler")
                .end()
                    .onException(AuthenticationException.class)
                    .handled(true)
                .to("direct:auth-error-handler")
                .end()
                    .onException(ServiceFall.class)
                    .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("X-User-ID"))
                .setHeader("X-Headers-Forbidden", constant("Email-Verified"))
                .to("direct:check-params")
                .process(searchUserProcessor)
                .process(changeSSOUserDataProcessor)
                .end();

        from("platform-http:/oapi/v1/accounts/username?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-username-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Username"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/username"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("username=${header.New-Username}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/nickname?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-nickname-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Nickname"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/nickname"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("nickname=${header.New-Nickname}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/email?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-email-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Email"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/email"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("email=${header.New-Email}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/password?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-password-route")
                .onException(NoRequiredHeader.class)
                .handled(true)
                    .to("direct:bad-request-error-handler")
                    .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Password"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/password"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("password=${header.New-Password}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi-inner/v1/accounts/update-account-state-sso?httpMethodRestrict=POST")
                .routeId("accounts-inner-update-account-state-sso-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                    .onException(AuthenticationException.class)
                    .handled(true)
                .to("direct:auth-error-handler")
                .end()
                    .onException(ServiceFall.class)
                    .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("X-User-ID"))
                .to("direct:check-params")
                .process(searchUserProcessor)
                .process(changeSSOUserAccountStateProcessor)
                .end();

        from("platform-http:/oapi/v1/accounts/description?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-description-route")
                    .onException(NoRequiredHeader.class)
                    .handled(true)
                .to("direct:bad-request-error-handler")
                .end()
                    .onException(AuthenticationException.class)
                    .handled(true)
                .to("direct:auth-error-handler")
                .end()
                    .onException(ServiceFall.class)
                    .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Description"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/description"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("description=${header.New-Description}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/birth-date?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-birth-date-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Birth-Date"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/birth-date"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("birth_date=${header.New-Birth-Date}"))
                .to("direct:sd-call-finalize");


        from("platform-http:/oapi-inner/v1/accounts/role?httpMethodRestrict=GET")
                .routeId("accounts-inner-get-account-role-sso-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("X-User-ID"))
                .to("direct:check-params")
                .process(searchUserProcessor)
                .process(getSSOUserAccountRoleProcessor)
                .setHeader("X-No-Meta", constant(true))
                .to("direct:finalize-request")
                .end();

        from("platform-http:/oapi/v1/accounts/role?httpMethodRestrict=GET")
                .routeId("accounts-get-account-role-sso-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Roles-Required", constant("profile-watch"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Integration"))
                .setHeader("X-Service-Request", simple("oapi-inner/v1/accounts/role"))
                .setBody(constant(""))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi-inner/v1/accounts/user/create-sso-account?httpMethodRestrict=POST")
                .routeId("accounts-inner-create-sso-account-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                    .onException(AuthenticationException.class)
                    .handled(true)
                .to("direct:auth-error-handler")
                .end()
                    .onException(ServiceFall.class)
                    .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("X-User-ID,Nickname,Email,Username"))
                .to("direct:check-params")
                .process(createSSOUserAccountProcessor)
                .end();

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=POST")
                .routeId("accounts-create-user-route")
                .setHeader("X-Headers-Required", constant("Nickname,Username,Email,Password"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("nickname=${header.Nickname}&username=${header.Username}&email=${header.Email}&password=${header.Password}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/image?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-image-route")
                .onException(NoRequiredHeader.class)
                    .handled(true)
                    .to("direct:bad-request-error-handler")
                .end()
                .onException(AuthenticationException.class)
                    .handled(true)
                    .to("direct:auth-error-handler")
                .end()
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("New-Image-Id"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}/image"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .setBody(simple("imglId=${header.New-Image-Id}"))
                .to("direct:sd-call-finalize");
    }
}