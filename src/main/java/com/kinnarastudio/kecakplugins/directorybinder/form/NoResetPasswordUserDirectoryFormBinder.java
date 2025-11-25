package com.kinnarastudio.kecakplugins.directorybinder.form;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormDeleteBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;
import org.joget.apps.form.model.FormStoreElementBinder;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.directory.dao.DepartmentDao;
import org.joget.directory.dao.EmploymentDao;
import org.joget.directory.dao.EmploymentReportToDao;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.dao.RoleDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.Employment;
import org.joget.directory.model.EmploymentReportTo;
import org.joget.directory.model.Role;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.directorybinder.exception.PasswordException;

/**
 * If the user is existing, it will not reset the password
 */

public class NoResetPasswordUserDirectoryFormBinder extends FormBinder implements FormStoreBinder, FormStoreElementBinder, FormDeleteBinder {
    private static final String LABEL = "(No Reset Password) User Directory Form Binder";

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        return resourceBundle.getString("buildNumber");
    }

    @Override
    public void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        Optional.ofNullable(rowSet)
                .stream()
                .flatMap(Collection::stream)
                .map(FormRow::getId)
                .forEach(userDao::deleteUser);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        final EmploymentDao employmentDao = (EmploymentDao) applicationContext.getBean("employmentDao");
        final EmploymentReportToDao employmentReportToDao = (EmploymentReportToDao) applicationContext.getBean("employmentReportToDao");
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        final DepartmentDao departmentDao = (DepartmentDao) applicationContext.getBean("departmentDao");
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

                    row.setId(primaryKey);

                    final String username = row.getProperty("username", row.getId());
                    final String firstName = row.getProperty("firstName");
                    final String lastName = row.getProperty("lastName");
                    final String email = row.getProperty("email");
                    final String strActive = row.getProperty("active", "true");
                    final int active = strActive.equals("true") || strActive.equals("active") || strActive.equals("1") ? 1 : 0;
                    final String locale = row.getProperty("locale");
                    final String telephoneNumber = row.getProperty("phoneNumber");
                    final String password = row.getProperty("password", "");
                    final String plainPassword = SecurityUtil.decrypt(password);
                    final String confirmPassword = row.getProperty("confirmPassword", "");
                    final String plainConfirmPassword = SecurityUtil.decrypt(confirmPassword);
                    final String organizationId = row.getProperty("organizationId");
                    final String departmentId = row.getProperty("departmentId");
                    final boolean isHod = "true".equals(row.getProperty("isHod"));
                    final String reportTo = row.getProperty("reportTo");

                    final Optional<User> optUser = Optional.ofNullable(primaryKey)
                            .filter(s -> !s.isEmpty())
                            .map(userDao::getUserById);

                    // update existing user
                    if (optUser.isPresent()) {
                        final User user = optUser.get();
                        user.setId(row.getId());
                        user.setUsername(username);
                        user.setFirstName(firstName);
                        user.setLastName(lastName);
                        user.setEmail(email);
                        user.setActive(active);
                        user.setLocale(locale);
                        user.setTelephoneNumber(telephoneNumber);
                        user.setRoles(new HashSet<Role>() {{
                            add(roleDao.getRole("ROLE_USER"));
                        }});

                        user.setConfirmPassword(user.getPassword());
                        userDao.updateUser(user);

                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    }

                    // create new user
                    else {
                        final boolean success = userDao.addUser(generatePassword(new User() {{
                            setId(row.getId());
                            setUsername(username);
                            setFirstName(firstName);
                            setLastName(lastName);
                            setEmail(email);
                            setActive(active);
                            setLocale(locale);
                            setTelephoneNumber(telephoneNumber);
                            setPassword(plainPassword);
                            setConfirmPassword(plainConfirmPassword);

                            setRoles(new HashSet<Role>() {{
                                add(roleDao.getRole("ROLE_USER"));
                            }});
                        }}));

                        if (!success) {
                            formData.addFormError(element.getPropertyString("id"), "Error adding user");
                            return null;
                        }

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    }

                    final User user = userDao.getUserById(row.getId());

                    final Set<Employment> employments = Optional.of(user)
                            .map(User::getEmployments)
                            .stream()
                            .flatMap(Collection<Employment>::stream)
                            .collect(Collectors.toSet());

                    if (employments.isEmpty()) {
                        final Employment newEmployment = new Employment() {{
                            setUserId(user.getId());

                            Optional.ofNullable(organizationId)
                                    .map(organizationDao::getOrganization)
                                    .ifPresent(this::setOrganization);

                            Optional.ofNullable(departmentId)
                                    .map(departmentDao::getDepartment)
                                    .ifPresent(this::setDepartment);
                        }};

                        employmentDao.addEmployment(newEmployment);

                        if (!reportTo.isEmpty()) {
                            Optional.of(reportTo)
                                    .map(userDao::getUserById)
                                    .map(User::getEmployments)
                                    .stream()
                                    .flatMap(Collection<Employment>::stream)
                                    .findFirst()
                                    .ifPresent(empReportTo -> employmentReportToDao.addEmploymentReportTo(new EmploymentReportTo() {{
                                        setSubordinate(newEmployment);
                                        setReportTo(empReportTo);
                                    }}));
                        }

                        if (isHod) {
                            Optional.of(departmentId)
                                    .map(departmentDao::getDepartment)
                                    .ifPresent(d -> {
                                        d.setHod(newEmployment);
                                        departmentDao.updateDepartment(d);
                                    });
                        }

                    } else {
                        if (!organizationId.isEmpty()) {
                            employmentDao.assignUserToOrganization(user.getId(), organizationId);
                        } else {
                            employments.stream()
                                    .map(Employment::getOrganizationId)
                                    .filter(Objects::nonNull)
                                    .forEach(id ->
                                            employmentDao.unassignUserFromOrganization(row.getId(), id));
                        }

                        employments.stream()
                                .map(Employment::getDepartmentId)
                                .filter(Objects::nonNull)
                                .forEach(Try.onConsumer(id -> employmentDao.unassignUserFromDepartment(user.getId(), id), (RuntimeException ignored) -> {
                                }));

                        if (!departmentId.isEmpty()) {
                            employmentDao.assignUserToDepartment(user.getId(), departmentId);
                        }

//                        if (isHod) {
//                            employmentDao.assignUserAsDepartmentHOD(user.getId(), departmentId);
//                        } else {
//                            employmentDao.unassignUserAsDepartmentHOD(user.getId(), departmentId);
//                        }

                        if (!reportTo.isEmpty()) {
                            employmentDao.assignUserReportTo(user.getId(), reportTo);
                        } else {
                            employmentDao.unassignUserReportTo(user.getId());
                        }
                    }

                    return (FormRowSet) new FormRowSet() {{
                        add(row);
                    }};
                }))
                .orElse(originalRowSet);
    }

    protected User generatePassword(final User user) throws PasswordException {
        final UserSecurity us = DirectoryUtil.getUserSecurity();

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return user;
        } else if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new PasswordException("Password does not match");
        }

        if (us != null) {
            user.setPassword(us.encryptPassword(user.getUsername(), user.getPassword()));
        } else {
            user.setPassword(StringUtil.md5Base16(user.getPassword()));
        }
        user.setConfirmPassword(user.getPassword());
        return user;
    }
}
