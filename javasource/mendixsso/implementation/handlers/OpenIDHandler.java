package mendixsso.implementation.handlers;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.ISession;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.http.ServletUtils;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.LogoutTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.SessionID;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.LogoutTokenValidator;
import mendixsso.implementation.ConfigurationManager;
import mendixsso.implementation.CustomSignupHintParameterMapper;
import mendixsso.implementation.SessionManager;
import mendixsso.implementation.UserMapper;
import mendixsso.implementation.error.IncompatibleUserTypeException;
import mendixsso.implementation.oidp.IdentityProviderMetaData;
import mendixsso.implementation.oidp.IdentityProviderMetaDataCache;
import mendixsso.implementation.utils.*;
import mendixsso.proxies.AuthRequest;
import mendixsso.proxies.ForeignIdentity;
import mendixsso.proxies.UserProfile;
import mendixsso.proxies.constants.Constants;
import system.proxies.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static mendixsso.implementation.utils.OpenIDUtils.getFingerPrint;

public class OpenIDHandler extends RequestHandler {

    public static final String OPENID_CLIENTSERVLET_LOCATION = "openid/";
    public static final String INDEX_PAGE = ConfigurationManager.getInstance().getIndexPage();
    public static final String CALLBACK = "callback";
    private static final String LOGIN = "login";
    private static final String LOGOFF = "logoff";
    private static final String BACKCHANNEL_LOGOUT = "backchannel_logout";
    private static final String OPENID_PROVIDER =
            ConfigurationManager.getInstance().getOpenIdConnectProvider();
    private static final boolean SSO_ENABLED = Constants.getSSOEnabled();
    private static final ILogNode LOG = Core.getLogger(Constants.getLogNode());
    private static final ILoginHandler loginHandler = new DefaultLoginHandler();
    private static final String FALLBACK_LOGINPAGE = "/login.html";
    private static final String CONTINUATION_PARAM = "continuation";
    private static final String ERROR_DESCRIPTION = "error_description";
    private boolean started;

    public OpenIDHandler() {
        if (!SSO_ENABLED) {
            LOG.info("NOT starting SSO handlers, disabled by configuration");
        } else {
            reconnectToMxID();
        }
    }

    private static AuthRequest retrieveAuthRequest(final IContext context, final String state) {
        return MendixUtils.retrieveSingleObjectFromDatabase(
                context,
                AuthRequest.class,
                "//%s[%s = $state]",
                Collections.singletonMap("state", state),
                AuthRequest.entityName,
                AuthRequest.MemberNames.State.name());
    }

    private void reconnectToMxID() {
        LOG.info("Starting OpenId handlers ... OpenIdProvider: " + OPENID_PROVIDER);
        try {
            started = true;
            LOG.info("Starting OpenId handlers ... DONE");

        } catch (Exception e) {
            LOG.error("Failed to discover OpenId service: " + e.getMessage(), e);
        }
    }

    @Override
    public void processRequest(IMxRuntimeRequest req, IMxRuntimeResponse resp, String path) {

        // always force expiration on this handlers!
        resp.addHeader("Expires", "0");

        final IContext context = Core.createSystemContext();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Incoming request: " + path + " fingerprint: " + getFingerPrint(req));
        }

        if (!SSO_ENABLED) {
            LOG.info(
                    "NOT handling SSO request, disabled by configuration, redirecting to login.html");
            redirect(resp, FALLBACK_LOGINPAGE);
            return;
        }

        if (!started) {
            reconnectToMxID();
        }

        if (!started) {
            LOG.error(
                    "NOT handling SSO request, could not connect to MxID, redirecting to login.html");
            redirect(resp, FALLBACK_LOGINPAGE);
            return;
        }
        try {
            if (LOGOFF.equalsIgnoreCase(path)) {
                logoff(req, resp); // requesting Single Sign Off (from client)
            } else if (BACKCHANNEL_LOGOUT.equalsIgnoreCase(path)) {
                backchannelLogout(req, resp, context); // requesting Single Sign Off (from server)
            } else if (LOGIN.equalsIgnoreCase(path)) {
                login(req, resp, context); // requesting authorization
            } else if (CALLBACK.equalsIgnoreCase(path)) {
                callback(req, resp, context); // redirect from open id provider
            } else {
                ErrorUtils.serveNotFoundError(req, resp, path);
            }
        } catch (InterruptedException itr) {
            Thread.currentThread().interrupt();
            if (req.getParameter(ERROR_DESCRIPTION) != null) {
                serveErrorPage(
                        req,
                        resp,
                        "An unexpected exception occurred while handling request : "
                                + req.getParameter(ERROR_DESCRIPTION),
                        false,
                        null,
                        ErrorUtils.ResponseType.OOPS);
            } else {
                serveErrorPage(
                        req,
                        resp,
                        "An unexpected exception occurred while handling request " + path,
                        false,
                        itr);
            }
        } catch (Exception e) {
            if (req.getParameter(ERROR_DESCRIPTION) != null) {
                serveErrorPage(
                        req,
                        resp,
                        "An unexpected exception occurred while handling request : "
                                + req.getParameter(ERROR_DESCRIPTION),
                        false,
                        null,
                        ErrorUtils.ResponseType.OOPS);
            } else {
                serveErrorPage(
                        req,
                        resp,
                        "An unexpected exception occurred while handling request " + path,
                        false,
                        e);
            }
        }
    }

    private void callback(IMxRuntimeRequest req, IMxRuntimeResponse resp, IContext context)
            throws URISyntaxException, ParseException, java.text.ParseException,
                    InterruptedException, IOException {
        LOG.debug("Callback from OpenID provider, evaluating..");

        final String requestURL = getFullURL(req.getHttpServletRequest());
        final AuthenticationResponse authResp =
                AuthenticationResponseParser.parse(new URI(requestURL));

        final State state = authResp.getState();

        // check state, get original request
        final AuthRequest originalRequest = retrieveAuthRequest(context, state.getValue());

        if (originalRequest == null) {
            throw new IllegalStateException("Unexpected OpenID callback unknown state");
        }

        if (authResp instanceof AuthenticationErrorResponse) {
            final ErrorObject error = ((AuthenticationErrorResponse) authResp).getErrorObject();

            final Prompt.Type promptType = Prompt.Type.parse(originalRequest.getPrompt());

            if (promptType == Prompt.Type.LOGIN) {
                throw new IllegalStateException(error.getDescription());
            } else {
                LOG.trace(
                        String.format("Authentication callback error: %s", error.getDescription()));
                OpenIDUtils.redirectToIndex(req, resp, originalRequest.getContinuation());
            }
        } else {

            final AuthenticationSuccessResponse successResponse =
                    (AuthenticationSuccessResponse) authResp;
            final Nonce expectedNonce = new Nonce(originalRequest.getNonce());
            final AuthorizationCode authCode = successResponse.getAuthorizationCode();

            /*
             * When an authorization code (using code or hybrid flow) has been
             * obtained, a token request can made to get the access token and the id
             * token:
             */
            final OIDCTokenResponse idTokenResponse = requestOIDCToken(req, authCode, state);
            final JWT idToken = getAndValidateIDToken(idTokenResponse, expectedNonce);

            final Map<String, Object> claims = idToken.getJWTClaimsSet().toJSONObject();
            final String userUUID = JSONObjectUtils.getString(claims, "sub");
            final String emailAddress = JSONObjectUtils.getString(claims, "email");

            // We assume that openId cannot be null since that would imply bigger issues higher up
            // in the
            // SSO stack.
            ForeignIdentityUtils.lockForeignIdentity(userUUID);
            try {
                final UserProfile userProfile = UserProfileUtils.getUserProfile(context, claims);
                loginHandler.onCompleteLogin(
                        context,
                        userProfile,
                        userUUID,
                        emailAddress,
                        idTokenResponse,
                        originalRequest.getContinuation(),
                        req,
                        resp);
            } finally {
                ForeignIdentityUtils.unlockForeignIdentity(userUUID);
            }
        }
    }

    private String getFullURL(HttpServletRequest request) {
        final StringBuffer requestURL = request.getRequestURL();
        final String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

    private OIDCTokenResponse requestOIDCToken(
            IMxRuntimeRequest req, AuthorizationCode authCode, State state)
            throws URISyntaxException, IOException, ParseException {

        final IdentityProviderMetaData discovered =
                IdentityProviderMetaDataCache.getInstance().getIdentityProviderMetaData();
        final URI redirectURI = new URI(OpenIDUtils.getRedirectUri(req));

        final ClientAuthentication clientAuthentication =
                new ClientSecretBasic(discovered.getClientId(), discovered.getClientSecret());

        final TokenRequest tokenReq =
                new TokenRequest(
                        discovered.getProviderMetadata().getTokenEndpointURI(),
                        clientAuthentication,
                        new AuthorizationCodeGrant(authCode, redirectURI),
                        new Scope(OIDCScopeValue.OFFLINE_ACCESS));
        LOG.debug(
                "Sending token request for state "
                        + state.getValue()
                        + " with auth code "
                        + authCode);

        final HTTPResponse tokenHTTPResp = tokenReq.toHTTPRequest().send();
        final TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp);

        if (tokenResponse instanceof TokenErrorResponse) {
            final ErrorObject error = ((TokenErrorResponse) tokenResponse).getErrorObject();
            throw new IllegalStateException(error.getDescription());
        }

        return (OIDCTokenResponse) tokenResponse;
    }

    private JWT getAndValidateIDToken(OIDCTokenResponse idTokenResponse, Nonce expectedNonce) {
        final JWT idToken = idTokenResponse.getOIDCTokens().getIDToken();
        try {
            // validate the ID token, see
            // http://connect2id.com/blog/how-to-validate-an-openid-connect-id-token
            IdentityProviderMetaDataCache.getInstance()
                    .getIdentityProviderMetaData()
                    .getIdTokenValidator()
                    .validate(idToken, expectedNonce);
        } catch (Exception e) {
            final ErrorObject errorObject =
                    OAuth2Error.SERVER_ERROR.appendDescription(" " + e.getMessage());
            throw new IllegalStateException(errorObject.getDescription());
        }

        return idToken;
    }

    private void login(IMxRuntimeRequest req, IMxRuntimeResponse resp, IContext context)
            throws CoreException, URISyntaxException, InterruptedException, IOException,
                    ParseException {
        final String continuation = req.getParameter(CONTINUATION_PARAM);
        detectContinuationJsInjection(continuation);

        // special case 1: already a valid session, do not bother with a new login
        final ISession session = this.getSessionFromRequest(req);
        if (session != null && !session.getUser(context).isAnonymous()) {
            LOG.debug("Existing session detected for non-anonymous user.");

            // Logout old session and initialize new session. This will allow for role changes to
            // take
            // effect.
            final ForeignIdentity foreignIdentity =
                    ForeignIdentityUtils.retrieveForeignIdentity(context, session);
            if (foreignIdentity == null) {
                // If the user was deleted in the mean time, we should trigger a authorize request
                // instead.
                LOG.debug(
                        "Could not find foreign identity for existing session, "
                                + "performing authorize request to re-login the user.");
                performAuthorizeRequest(req, resp, context, continuation, Prompt.Type.LOGIN);
                return;
            }

            ForeignIdentityUtils.lockForeignIdentity(foreignIdentity.getUUID());

            try {
                final User user = foreignIdentity.getForeignIdentity_User();
                if (!UserMapper.getInstance().isCompatibleUserType(user)) {
                    // Because of a change to a new user entity type, it can happen that an old user
                    // of an
                    // incompatible type is provided
                    throw new IncompatibleUserTypeException(user.getMendixObject().getType());
                }
                loginHandler.onAlreadyHasSession(
                        context, user, session, foreignIdentity.getUUID(), continuation, req, resp);
            } catch (IncompatibleUserTypeException e) {
                // We forcefully logoff the user in order to re-trigger all login logic which will
                // take care
                // of incompatible user type
                logoff(req, resp);
            } finally {
                ForeignIdentityUtils.unlockForeignIdentity(foreignIdentity.getUUID());
            }
        } else {
            final Optional<String> promptParam = Optional.ofNullable(req.getParameter("prompt"));
            final String promptValue = promptParam.orElse(Prompt.Type.NONE.toString());
            final Prompt.Type promptType;
            if (ConfigurationManager.getInstance().getSilentAuthentication()
                    && promptValue.equals(Prompt.Type.NONE.toString())) {
                promptType = Prompt.Type.NONE;
            } else {
                promptType = Prompt.Type.LOGIN;
            }
            performAuthorizeRequest(req, resp, context, continuation, promptType);
        }
    }

    private void performAuthorizeRequest(
            IMxRuntimeRequest req,
            IMxRuntimeResponse resp,
            IContext context,
            String continuation,
            final Prompt.Type promptType)
            throws CoreException, URISyntaxException, IOException, ParseException {
        LOG.debug("Incoming login request, redirecting to OpenID provider");

        final State state = new State();
        LOG.debug("OIDC Login process started for, state: " + state.getValue());

        // Initialize or generate nonce
        final Nonce nonce = new Nonce();
        // Specify scope
        final Scope scope =
                Scope.parse(ConfigurationManager.getInstance().getOpenIDConnectScopes());
        final IdentityProviderMetaData discovered =
                IdentityProviderMetaDataCache.getInstance().getIdentityProviderMetaData();
        // Compose the request
        final String signupHint = CustomSignupHintParameterMapper.getInstance().getSignupHint(context,
            SystemHttpRequestBuilder.build(context,req));

        final URI redirectURI = new URI(OpenIDUtils.getRedirectUri(req));

        final AuthenticationRequest.Builder authenticationRequestBuilder =
                new AuthenticationRequest.Builder(
                                discovered.getResponseType(),
                                scope,
                                discovered.getClientId(),
                                redirectURI)
                        .endpointURI(discovered.getProviderMetadata().getAuthorizationEndpointURI())
                        .state(state)
                        .nonce(nonce)
                        .prompt(new Prompt(promptType));

        if (signupHint != null && !signupHint.equals("")) {
            authenticationRequestBuilder.customParameter("signup_hint", signupHint);
        }

        final URI authReqURI = authenticationRequestBuilder.build().toURI();

        // store in the database
        final AuthRequest authReq = new AuthRequest(context);
        authReq.setState(state.getValue());
        authReq.setNonce(nonce.getValue());
        authReq.setContinuation(continuation);
        authReq.setPrompt(promptType.toString());
        authReq.commit();

        LOG.debug("Redirecting to " + authReqURI.toString());

        redirect(resp, authReqURI.toString());
    }

    private void logoff(IMxRuntimeRequest req, IMxRuntimeResponse resp) throws CoreException {
        final String continuation = req.getParameter(CONTINUATION_PARAM);
        detectContinuationJsInjection(continuation);

        final ISession session = this.getSessionFromRequest(req);
        if (session != null) {
            Core.logout(session);
        }

        OpenIDUtils.redirectToIndex(req, resp, continuation);
    }

    private void detectContinuationJsInjection(String url) {
        if (url != null && url.trim().toLowerCase().startsWith("javascript:")) {
            throw new IllegalArgumentException(
                    "Javascript injection detected in parameter '" + CONTINUATION_PARAM + "'");
        }
    }

    private void backchannelLogout(
            IMxRuntimeRequest request, IMxRuntimeResponse response, IContext context) {
        try {
            final BackChannelLogoutRequest backChannelLogoutRequest =
                    BackChannelLogoutRequest.parse(
                            ServletUtils.createHTTPRequest(request.getHttpServletRequest()));

            final OIDCProviderMetadata oidcProviderMetadata =
                    IdentityProviderMetaDataCache.getInstance()
                            .getIdentityProviderMetaData()
                            .getProviderMetadata();
            final LogoutTokenValidator validator =
                    new LogoutTokenValidator(
                            oidcProviderMetadata.getIssuer(),
                            new ClientID(ConfigurationManager.getInstance().getEnvironmentUUID()),
                            JWSAlgorithm.RS256,
                            oidcProviderMetadata.getJWKSetURI().toURL());
            validator.setMaxClockSkew(
                    ConfigurationManager.getInstance().getTokenValidatorMaxClockSkew().intValue());

            final JWT logoutToken = backChannelLogoutRequest.getLogoutToken();
            final LogoutTokenClaimsSet claimsSet = validator.validate(logoutToken);
            final SessionID sessionID = claimsSet.getSessionID();
            if (sessionID == null) {
                SessionManager.logoutAllSessionsOfForeignIdentity(
                        context, claimsSet.getSubject().getValue());
            } else {
                SessionManager.logoutSession(context, sessionID.getValue());
            }
        } catch (Exception e) {
            LOG.error(
                    "An unexpected exception occurred while handling a back-channel logout request.",
                    e);
            response.setStatus(IMxRuntimeResponse.INTERNAL_SERVER_ERROR);
            return;
        }

        response.setStatus(IMxRuntimeResponse.OK);
        response.setContentType("text/plain");
    }

    private void redirect(IMxRuntimeResponse resp, String url) {
        resp.setStatus(IMxRuntimeResponse.SEE_OTHER);
        resp.addHeader("location", url);
    }

    private void serveErrorPage(
            final IMxRuntimeRequest req,
            final IMxRuntimeResponse resp,
            final String message,
            final boolean messageIncludesHtml,
            Exception e) {
        serveErrorPage(
                req,
                resp,
                message,
                messageIncludesHtml,
                e,
                ErrorUtils.ResponseType.INTERNAL_SERVER_ERROR);
    }

    private void serveErrorPage(
            final IMxRuntimeRequest req,
            final IMxRuntimeResponse resp,
            final String message,
            final boolean messageIncludesHtml,
            Exception e,
            ErrorUtils.ResponseType responseType) {
        ErrorUtils.serveError(req, resp, responseType, message, messageIncludesHtml, e);
    }
}
