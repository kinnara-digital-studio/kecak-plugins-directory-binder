package com.kinnarastudio.kecakplugins.directoryformbinder.form;

import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.directory.dao.EmploymentDao;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.dao.RoleDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.*;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author aristo
 * <p>
 * Store data into directory table <b>dir_user</b> and <b>dir_employment</b>.
 * <br/>
 * Build in properties are:
 * <ul>
 *     <li>Table <b>dir_user</b></li>
 *     <ul>
 *         <li>id</li>
 *         <li>username</li>
 *         <li>firstName</li>
 *         <li>lastName</li>
 *         <li>email</li>
 *         <li>active</li>
 *         <li>locale</li>
 *         <li>telephone_number</li>
 *     </ul>
 *     <li>Table <b>dir_employment</b></li>
 *     <ul>
 *         <li>organizationId</li>
 *     </ul>
 * </ul>
 */
public class UserDirectoryFormBinder extends DefaultFormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder {
    public final static String LABEL = "User Directory Form Binder";

    @Override
    public String getFormId() {
        final Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_ID);
    }

    @Override
    public String getTableName() {
        return "dir_user";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        return Optional.ofNullable(primaryKey)
                .map(userDao::getUserById)
                .map(user -> {
                    final FormRow row = new FormRow();

                    row.setId(user.getId());
                    row.setProperty("username", Optional.ofNullable(user.getUsername()).orElse(""));
                    row.setProperty("firstName", Optional.ofNullable(user.getFirstName()).orElse(""));
                    row.setProperty("lastName", Optional.ofNullable(user.getLastName()).orElse(""));
                    row.setProperty("email", Optional.ofNullable(user.getEmail()).orElse(""));
                    row.setProperty("active", user.getActive() != 1 ? "false" : "true");
                    row.setProperty("locale", Optional.ofNullable(user.getLocale()).orElse(""));
                    row.setProperty("telephone_number", Optional.ofNullable(user.getTelephoneNumber()).orElse(""));

                    Optional.of(user)
                            .map(User::getEmployments)
                            .stream()
                            .flatMap(Collection<Employment>::stream)
                            .findFirst()
                            .map(Employment::getOrganizationId)
                            .ifPresent(s -> row.setProperty("organizationId", s));

                    final FormRowSet result = new FormRowSet();
                    result.add(row);
                    return result;
                })
                .orElseGet(FormRowSet::new);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        return Optional.ofNullable(originalRowSet)
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .map(Try.onFunction(row -> {
                    final String primaryKey = Optional.of(row)
                            .map(FormRow::getId)
                            .orElseGet(formData::getPrimaryKeyValue);

                    @Nullable User user = Optional.ofNullable(primaryKey)
                            .filter(s -> !s.isEmpty())
                            .map(userDao::getUserById)
                            .orElse(null);

                    @Nullable Organization organization = Optional.ofNullable(row.getProperty("organizationId"))
                            .map(organizationDao::getOrganization)
                            .orElse(null);

                    final String active = row.getProperty("active", "true");
                    final String password = row.getProperty("password", "");
                    final String confirmPassword = row.getProperty("confirm_password", "");

                    if (user == null) {
                        user = new User();
                        user.setId(row.getId());
                        user.setUsername(row.getProperty("username", row.getId()));
                        user.setFirstName(row.getProperty("firstName"));
                        user.setLastName(row.getProperty("lastName"));
                        user.setEmail(row.getProperty("email"));
                        user.setActive(active.equals("true") || active.equals("active") || active.equals("1") ? 1 : 0);
                        user.setLocale(row.getProperty("locale"));
                        user.setTelephoneNumber(row.getProperty("telephone_number"));

                        updatePassword(user, password, confirmPassword);

                        userDao.addUser(user);

                        Optional.of("roleId")
                                .map(s -> row.getProperty(s, "ROLE_USER"))
                                .map(roleDao::getRole)
                                .map(Collections::singleton)
                                .ifPresent(user::setRoles);

                        if (organization != null) {
                            setEmployment(user, organization);
                        }

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    } else {
                        user.setId(row.getId());
                        user.setUsername(row.getProperty("username", row.getId()));
                        user.setFirstName(row.getProperty("firstName"));
                        user.setLastName(row.getProperty("lastName"));
                        user.setEmail(row.getProperty("email"));
                        user.setActive("true".equals(active) || "active".equals(active) || "1".equals(active) ? 1 : 0);
                        user.setLocale(row.getProperty("locale"));
                        user.setTelephoneNumber(row.getProperty("telephone_number"));

                        updatePassword(user, password, confirmPassword);
//                        optRole.map(Collections::singleton).ifPresent(user::setRoles);

                        if (organization != null) {
                            setEmployment(user, organization);
                        }

                        userDao.updateUser(user);

                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    }

                    final FormRowSet result = new FormRowSet();
                    result.add(row);
                    return result;
                })).orElse(originalRowSet);
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Nonnull
    protected User setEmployment(@Nonnull User user, @Nonnull Organization organization) {
        final EmploymentDao employmentDao = (EmploymentDao) AppUtil.getApplicationContext().getBean("employmentDao");

        final Employment employment = Optional.of(user)
                .map(User::getEmployments).stream()
                .flatMap(Collection<Employment>::stream)
                .findFirst()
                .orElseGet(() -> {
                    Employment newEmployment = new Employment();
                    newEmployment.setId(UUID.randomUUID().toString());
                    newEmployment.setUserId(user.getUsername());
                    newEmployment.setOrganizationId(organization.getId());

                    employmentDao.addEmployment(newEmployment);
                    return newEmployment;
                });

        employmentDao.assignUserToOrganization(employment.getUserId(), organization.getId());

        return user;
    }

    protected void updatePassword(User user, String password, String confirmPassword) {
        final UserSecurity us = DirectoryUtil.getUserSecurity();

        if (!password.isEmpty() && password.equals(confirmPassword)) {
            user.setPassword(us.encryptPassword(user.getUsername(), password));
        }
    }
}
