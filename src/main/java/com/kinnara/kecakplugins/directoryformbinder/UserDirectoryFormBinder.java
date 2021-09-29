package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.HashSalt;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PasswordGeneratorUtil;
import org.joget.directory.dao.EmploymentDao;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.Employment;
import org.joget.directory.model.Organization;
import org.joget.directory.model.User;
import org.joget.directory.model.UserSalt;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author aristo
 *
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
    @Override
    public String getFormId() {
        Form form = FormUtil.findRootForm(getElement());
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
        final FormRowSet results = new FormRowSet();

        Optional.ofNullable(primaryKey)
                .map(userDao::getUserById)
                .ifPresent(user -> {
                    FormRow row = new FormRow();
                    row.setId(user.getId());

                    row.setProperty("username", Optional.ofNullable(user.getUsername()).orElse(""));
                    row.setProperty("firstName", Optional.ofNullable(user.getFirstName()).orElse(""));
                    row.setProperty("lastName", Optional.ofNullable(user.getLastName()).orElse(""));
                    row.setProperty("email", Optional.ofNullable(user.getEmail()).orElse(""));
                    String active = "true";
                    if (user.getActive() != 1) {
                        active = "false";
                    }
                    row.setProperty("active", active);
                    row.setProperty("locale", Optional.ofNullable(user.getLocale()).orElse(""));
                    row.setProperty("telephone_number", Optional.ofNullable(user.getTelephoneNumber()).orElse(""));

                    Optional.of(user)
                            .map(User::getEmployments)
                            .map(Collection<Employment>::stream)
                            .orElseGet(Stream::empty)
                            .findFirst()
                            .map(Employment::getOrganizationId)
                            .ifPresent(s -> row.setProperty("organizationId", s));


                    results.add(row);
                });

        return results;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet formRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        Optional.ofNullable(formRowSet)
                .map(FormRowSet::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .ifPresent(row -> {
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

                    UserSecurity us = DirectoryUtil.getUserSecurity();
                    UserSalt userSalt = new UserSalt();
                    if (user == null) {
                        user = new User();
                        user.setId(row.getId());
                        user.setUsername(row.getProperty("username"));
                        user.setFirstName(row.getProperty("firstName"));
                        user.setLastName(row.getProperty("lastName"));
                        user.setEmail(row.getProperty("email"));
                        if (active.equals("true")
                                || active.equals("active")
                                || active.equals("1")) {
                            user.setActive(1);
                        } else {
                            user.setActive(0);
                        }
                        user.setLocale(row.getProperty("locale"));
                        user.setTelephoneNumber(row.getProperty("telephone_number"));
                        if (!password.isEmpty() && password.equals(row.getProperty("confirm_password", ""))) {
                            if (us != null) {
                                user.setPassword(us.encryptPassword(user.getUsername(), row.getProperty("password")));
                            } else {
                                try {
                                    HashSalt hashSalt = PasswordGeneratorUtil.createNewHashWithSalt(row.getProperty("password"));
                                    userSalt.setId(UUID.randomUUID().toString());
                                    userSalt.setRandomSalt(hashSalt.getSalt());

                                    user.setPassword(hashSalt.getHash());
                                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                                    LogUtil.error(getClassName(), e, e.getMessage());
                                }
                            }
                        }

                        user.setDateCreated(now);
                        user.setCreatedBy(currentUser);
                        userDao.addUser(user);

                        if (organization != null) {
                            setEmployment(user, organization);
                        }

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    } else {
                        user.setUsername(row.getProperty("username"));
                        user.setFirstName(row.getProperty("firstName"));
                        user.setLastName(row.getProperty("lastName"));
                        user.setEmail(row.getProperty("email"));

                        if ("true".equals(active)
                                || "active".equals(active)
                                || "1".equals(active)) {
                            user.setActive(1);
                        } else {
                            user.setActive(0);
                        }
                        user.setLocale(row.getProperty("locale"));
                        user.setTelephoneNumber(row.getProperty("telephone_number"));
                        user.setDateModified(row.getDateModified());
                        user.setModifiedBy(row.getModifiedBy());
                        if (!password.isEmpty() && password.equals(row.getProperty("confirm_password", ""))) {
                            if (us != null) {
                                user.setPassword(us.encryptPassword(user.getUsername(), row.getProperty("password")));
                            } else {
                                try {
                                    HashSalt hashSalt = PasswordGeneratorUtil.createNewHashWithSalt(password);
                                    userSalt.setId(UUID.randomUUID().toString());
                                    userSalt.setRandomSalt(hashSalt.getSalt());

                                    user.setPassword(hashSalt.getHash());
                                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                                    LogUtil.error(getClassName(), e, e.getMessage());
                                }
                            }
                        }

                        userDao.updateUser(user);

                        if (organization != null) {
                            setEmployment(user, organization);
                        }

                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    }
                });

        return formRowSet;
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "User Directory Form Binder";
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

        Employment employment = Optional.of(user)
                .map(User::getEmployments)
                .map(Collection<Employment>::stream)
                .orElseGet(Stream::empty)
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
}
