package nl.finalist.liferay.oidc.providers;

import java.util.Map;

public class AzureADDefault extends UserInfoProvider {

    @Override
    public String getEmail(Map<String, String> userInfo) {
        return userInfo.get("email");
    }

    @Override
    public String getFirstName(Map<String, String> userInfo) {
        return userInfo.get("given_name");
    }

    @Override
    public String getLastName(Map<String, String> userInfo) {
        return userInfo.get("family_name");
    }
}
