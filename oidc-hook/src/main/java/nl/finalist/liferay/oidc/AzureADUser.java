package nl.finalist.liferay.oidc;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;

import java.util.ArrayList;
import java.util.List;

public class AzureADUser {
    private static final Log LOG = LogFactoryUtil.getLog(AzureADUser.class);
    private User user;
    private ArrayList<AzureADGroup> azureADGroups;

    public AzureADUser(User liferayUser, ArrayList<AzureADGroup> azureADGroupslist) {
        this.user = liferayUser;
        this.azureADGroups = azureADGroupslist;
    }

    public void showUserGroups() throws SystemException {
        if (user != null) {
            List<UserGroup> userGroups = user.getUserGroups();
            for (int i = 0; i < userGroups.size(); i++) {
                LOG.debug(userGroups.get(i).getUserGroupId());
                LOG.debug(userGroups.get(i).getName());
            }
        } else {
            LOG.debug("no user found");
        }
    }

    public boolean createOrUpdateUserGroups() {
        boolean completed = false;
        if (areUserGroupsSync()) {
            LOG.debug("login...");
            completed = true;
        } else {
            syncUserGroups();
            unassignUnnecessaryUserGroupsFromLiferayUser();
            addNecessaryUserGroupsToLiferayUser();
            if (areUserGroupsSync()) {
                LOG.debug("login after user sync...");
                completed = true;
            }
        }
        return completed;
    }

    public void unassignUnnecessaryUserGroupsFromLiferayUser() {
        try {
            LOG.debug("unassign unnecessary usergroups from liferay user :: " + user.getUserId() + " :: " + user.getLogin());
            ArrayList<UserGroup> userGroupsList = findAzureADGroupNotNeededForUser();
            for (UserGroup userGroup : userGroupsList) {
                UserLocalServiceUtil.unsetUserGroupUsers(userGroup.getUserGroupId(), new long[]{user.getUserId()});
                LOG.debug("::=>" + userGroup.getName());
            }
        } catch (PortalException | SystemException ex) {
            LOG.error("error :: unassignUnnecessaryUserGroupsFromLiferayUser :: ", ex);
        }
    }

    public void addNecessaryUserGroupsToLiferayUser() {
        try {
            LOG.debug("adding necessary usergroups from liferay user :: " + user.getUserId() + " :: " + user.getLogin());
            ArrayList<UserGroup> userGroupsList = findAzureADGroupNeededForUser();
            for (UserGroup userGroup : userGroupsList) {
                UserLocalServiceUtil.addUserGroupUser(userGroup.getUserGroupId(), user);
                LOG.debug("::=>" + userGroup.getName());
            }
        } catch (PortalException | SystemException ex) {
            LOG.error("error :: addNecessaryUserGroupsToLiferayUser :: ", ex);
        }
    }

    private void syncUserGroups() {
        try {
            ArrayList<UserGroup> userGroupsList = new ArrayList<>(UserGroupLocalServiceUtil.getUserGroups(user.getCompanyId()));
            for (AzureADGroup azUserGroup : azureADGroups) {
                boolean found = false;
                for (UserGroup lfuserGroup : userGroupsList) {
                    if (azUserGroup.getName().equals(lfuserGroup.getName())) {
                        found = true;
                    }
                }
                if (!found) {
                    UserGroupLocalServiceUtil.addUserGroup(user.getUserId(),
                            user.getCompanyId(),
                            azUserGroup.getName(),
                            azUserGroup.getObjectId(),
                            getServiceContext());
                }
            }
        } catch (PortalException | SystemException ex) {
            LOG.error("error :: syncUserGroups :: ", ex);
        }
    }

    private ServiceContext getServiceContext() {
        ServiceContext serviceContext = new ServiceContext();
        return serviceContext;
    }

    public ArrayList<UserGroup> findAzureADGroupNeededForUser() {
        ArrayList<UserGroup> result = new ArrayList<>();
        try {
            ArrayList<UserGroup> userGroupsList = new ArrayList<>(UserGroupLocalServiceUtil.getUserGroups(user.getCompanyId()));
            for (UserGroup lfuserGroup : userGroupsList) {
                boolean found = false;
                for (AzureADGroup azUserGroup : azureADGroups) {
                    if (azUserGroup.getName().equals(lfuserGroup.getName())) {
                        found = true;
                    }
                }
                if (found && !UserLocalServiceUtil.hasUserGroupUser(lfuserGroup.getUserGroupId(), user.getUserId())) {
                    result.add(lfuserGroup);
                }
            }
        } catch (SystemException ex) {
            LOG.error("error :: findAzureADGroupNeededForUser :: ", ex);
        }
        return result;
    }

    public ArrayList<UserGroup> findAzureADGroupNotNeededForUser() {
        ArrayList<UserGroup> result = new ArrayList<>();
        try {
            ArrayList<UserGroup> userGroupsList = new ArrayList<>(user.getUserGroups());
            for (UserGroup lfuserGroup : userGroupsList) {
                boolean found = false;
                for (AzureADGroup azUserGroup : azureADGroups) {
                    if (azUserGroup.getName().equals(lfuserGroup.getName())) {
                        found = true;
                    }
                }
                if (!found) {
                    result.add(lfuserGroup);
                }
            }
        } catch (SystemException ex) {
            LOG.debug("error :: findAzureADGroupNotNeededForUser :: ", ex);
        }
        return result;
    }

    public boolean areUserGroupsSync() {
        return findAzureADGroupNeededForUser().size() == 0 &&
                findAzureADGroupNotNeededForUser().size() == 0;
    }
}
