package nl.finalist.liferay.oidc.providers;

import java.util.Map;

public class AzureAD extends UserInfoProvider {

	@Override
	public String getEmail(Map<String, String> userInfo) {
		return userInfo.get("unique_name");
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
