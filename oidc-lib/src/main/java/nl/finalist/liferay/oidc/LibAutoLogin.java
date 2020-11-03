package nl.finalist.liferay.oidc;

import nl.finalist.liferay.oidc.providers.UserInfoProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * AutoLogin for OpenID Connect 1.0
 * This class should be used in tandem with the OpenIDConnectFilter. That filter will do the OAuth conversation and
 * set a session attribute containing the UserInfo object (the claims).
 * This AutoLogin will use the claims to find a corresponding Liferay user or create a new one if none found.
 */
public class LibAutoLogin {

    private final LiferayAdapter liferay;

    public LibAutoLogin(LiferayAdapter liferay) {
        this.liferay = liferay;
        liferay.info("Initialized LibAutoLogin with Liferay API: " + liferay.getClass().getName());
    }

    public String[] doLogin(HttpServletRequest request, HttpServletResponse response) {
        String[] userResponse = null;

        long companyId = liferay.getCompanyId(request);

        OIDCConfiguration oidcConfiguration = liferay.getOIDCConfiguration(companyId);

        if (oidcConfiguration.isEnabled()) {
            HttpSession session = request.getSession();
            Map<String, String> userInfo = (Map<String, String>) session.getAttribute(
                    LibFilter.OPENID_CONNECT_SESSION_ATTR);
            String userGroups = (String) session.getAttribute(LibFilter.OPENID_CONNECT_UG_SESSION_ATTR);
            UserInfoProvider provider = ProviderFactory.getOpenIdProvider(oidcConfiguration.providerType());

            if (userInfo == null) {
                // Normal flow, apparently no current OpenID conversation
                liferay.trace("No current OpenID Connect conversation, no auto login");
                try {
                    liferay.trace("Redirecting sso login");
                    redirectToLogin(request, response, oidcConfiguration);
                } catch (IOException e) {
                    throw new RuntimeException("While redirecting to sso login", e);
                }
            } else if (StringUtils.isBlank(provider.getEmail(userInfo))) {
                liferay.error("Unexpected: OpenID Connect UserInfo does not contain email field. " +
                        "Cannot correlate to Liferay user. UserInfo: " + userInfo);
            } else {
                liferay.trace("Found OpenID Connect session attribute, userinfo: " + userInfo);
                String emailAddress = provider.getEmail(userInfo);
                String givenName = provider.getFirstName(userInfo);
                String familyName = provider.getLastName(userInfo);

                String userId = liferay.createOrUpdateUser(companyId, emailAddress, givenName, familyName);
                liferay.trace("Returning credentials for userId " + userId + ", email: " + emailAddress);
                liferay.setUserGroups(Long.parseLong(userId), userGroups);

                userResponse = new String[]{userId, UUID.randomUUID().toString(), "false"};
            }
        } else {
            liferay.trace("OpenIDConnectAutoLogin not enabled for this virtual instance. Will skip it.");
        }

        return userResponse;
    }

    private String getRedirectLoginUri(HttpServletRequest request) {
        String completeURL = liferay.getPortalURL(request);
        return completeURL.replaceAll("\\?.*", "")+"/c/portal/login";
    }

    private String generateStateParam(HttpServletRequest request) {
        return DigestUtils.md5Hex(request.getSession().getId());
    }

    private void redirectToLogin(HttpServletRequest request,
                                 HttpServletResponse response,
                                 OIDCConfiguration oidcConfiguration) throws IOException {
        try {
            OAuthClientRequest oAuthRequest = OAuthClientRequest
                    .authorizationLocation(oidcConfiguration.authorizationLocation())
                    .setClientId(oidcConfiguration.clientId())
                    .setRedirectURI(getRedirectLoginUri(request))
                    .setResponseType("code")
                    .setScope(oidcConfiguration.scope())
                    .setState(generateStateParam(request))
                    .buildQueryMessage();
            liferay.debug("Redirecting to URL: " + oAuthRequest.getLocationUri());
            response.sendRedirect(oAuthRequest.getLocationUri());
        } catch (OAuthSystemException e) {
            throw new IOException("While redirecting to OP for SSO login", e);
        }
    }
}
