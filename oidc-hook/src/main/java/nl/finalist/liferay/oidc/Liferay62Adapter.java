package nl.finalist.liferay.oidc;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.PwdGenerator;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class Liferay62Adapter implements LiferayAdapter {

    private static final Log LOG = LogFactoryUtil.getLog(Liferay62Adapter.class);

    @Override
    public OIDCConfiguration getOIDCConfiguration(long companyId) {
        return new OpenIDConnectPortalPropsConfiguration(companyId);
    }

    @Override
    public void trace(String s) {
        LOG.trace(s);
    }

    @Override
    public void info(String s) {
        LOG.info(s);
    }

    @Override
    public void debug(String s) {
        LOG.debug(s);
    }

    @Override
    public void warn(String s) {
        LOG.warn(s);
    }

    @Override
    public String getCurrentCompleteURL(HttpServletRequest request) {
        return PortalUtil.getCurrentCompleteURL(request);
    }

    @Override
    public boolean isUserLoggedIn(HttpServletRequest request) {
        try {
            return PortalUtil.getUser(request) != null;
        } catch (PortalException | SystemException e) {
            return false;
        }
    }

    @Override
    public long getCompanyId(HttpServletRequest request) {
        return PortalUtil.getCompanyId(request);
    }

    @Override
    public void error(String s) {
        LOG.error(s);
    }

    @Override
    public String createOrUpdateUser(long companyId, String emailAddress, String firstName, String lastName) {

        try {
            User user = UserLocalServiceUtil.fetchUserByEmailAddress(companyId, emailAddress);

            if (user == null) {
                LOG.debug("No Liferay user found with email address " + emailAddress + ", will create one.");
                user = addUser(companyId, emailAddress, firstName, lastName);
            } else {
                LOG.debug("User found, updating name details with info from userinfo");
                updateUser(user, firstName, lastName);
            }
            return String.valueOf(user.getUserId());

        } catch (SystemException | PortalException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUserGroups(long userId, String userGroups) {
        try {
            JSONObject jsonObject = new JSONObject(userGroups);
            if (jsonObject.getInt("responseCode") == 200) {
                JSONArray groups = jsonObject.getJSONObject("responseMsg").getJSONArray("value");
                ArrayList<AzureADGroup> list = new ArrayList<AzureADGroup>();
                for (int i = 0; i < groups.length(); i++) {
                    if (groups.getJSONObject(i) != null) {
                        LOG.debug(getJSONString(groups.getJSONObject(i), "displayName"));
                        list.add(new AzureADGroup.AzureADGroupBuilder(getJSONString(groups.getJSONObject(i), "displayName"))
                                .objectId(getJSONString(groups.getJSONObject(i), "id"))
                                .description(getJSONString(groups.getJSONObject(i), "description"))
                                .mail(getJSONString(groups.getJSONObject(i), "mail"))
                                .build());
                    }
                }
                User user = UserLocalServiceUtil.getUser(userId);
                AzureADUser azureADUser = new AzureADUser(user, list);
                azureADUser.showUserGroups();
                azureADUser.createOrUpdateUserGroups();
                azureADUser.showUserGroups();
            } else {
                this.error(jsonObject.getInt("responseCode") + " :: " + jsonObject.getJSONObject("responseMsg").toString());
            }
        } catch (SystemException | PortalException e) {
            throw new RuntimeException(e);
        }
    }

    private String getJSONString(JSONObject jsonObject, String propertyName) {
        try {
            return jsonObject.get(propertyName) == null ? "" : jsonObject.getString(propertyName);
        } catch (Exception e) {
            return "";
        }
    }

    // Copied from OpenSSOAutoLogin.java
    protected User addUser(
            long companyId, String emailAddress, String firstName, String lastName)
            throws SystemException, PortalException {

        Locale locale = LocaleUtil.getMostRelevantLocale();
        long creatorUserId = 0;
        boolean autoPassword = false;
        String password1 = PwdGenerator.getPassword();
        String password2 = password1;
        boolean autoScreenName = true;
        String screenName = "not_used_but_autogenerated_instead";
        long facebookId = 0;
        String openId = StringPool.BLANK;
        String middleName = StringPool.BLANK;
        int prefixId = 0;
        int suffixId = 0;
        boolean male = true;
        int birthdayMonth = Calendar.JANUARY;
        int birthdayDay = 1;
        int birthdayYear = 1970;
        String jobTitle = StringPool.BLANK;
        long[] groupIds = null;
        long[] organizationIds = null;
        long[] roleIds = null;
        long[] userGroupIds = null;
        boolean sendEmail = false;
        ServiceContext serviceContext = new ServiceContext();

        User user = UserLocalServiceUtil.addUser(
                creatorUserId, companyId, autoPassword, password1, password2,
                autoScreenName, screenName, emailAddress, facebookId, openId,
                locale, firstName, middleName, lastName, prefixId, suffixId, male,
                birthdayMonth, birthdayDay, birthdayYear, jobTitle, groupIds,
                organizationIds, roleIds, userGroupIds, sendEmail, serviceContext);

        // No password
        user.setPasswordReset(false);
        // No reminder query at first login.
        user.setReminderQueryQuestion("x");
        user.setReminderQueryAnswer("y");
        UserLocalServiceUtil.updateUser(user);
        return user;
    }


    private void updateUser(User user, String firstName, String lastName) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        try {
            UserLocalServiceUtil.updateUser(user);
        } catch (SystemException e) {
            LOG.error("Could not update user with new name attributes", e);
        }
    }
}
