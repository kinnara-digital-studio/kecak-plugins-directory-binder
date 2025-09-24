package com.kinnarastudio.kecakplugins.directorybinder.form;

import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.joget.apps.app.dao.EnvironmentVariableDao;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormDeleteBinder;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;
import org.joget.apps.form.model.FormStoreElementBinder;
import org.joget.directory.dao.DepartmentDao;
import org.joget.directory.dao.EmploymentDao;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.Department;
import org.joget.directory.model.Employment;
import org.joget.directory.model.Organization;
import org.joget.directory.model.User;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

public class DepartmentDirectoryFormBinder extends FormBinder implements FormLoadBinder, FormStoreBinder, FormLoadElementBinder,
        FormStoreElementBinder, FormDeleteBinder {

    public final static String LABEL = "Department Directory Form Binder";

    @Override
    public void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final DepartmentDao departmentDao = (DepartmentDao) applicationContext.getBean("departmentDao");

        Optional.ofNullable(rowSet)
                .stream()
                .flatMap(Collection::stream)
                .map(FormRow::getId)
                .forEach(departmentDao::deleteDepartment);
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final DepartmentDao departmentDao = (DepartmentDao) applicationContext.getBean("departmentDao");

        final String fieldId = "id";
        final String fieldName = "name";
        final String fieldOrgId = "organizationId";
        final String fieldParentDeptId = "parentId";
        final String fieldHod = "hod";

        return Optional.ofNullable(primaryKey)
                .map(departmentDao::getDepartment)
                .map(d -> (FormRow) new FormRow() {{
                    setProperty(fieldId, d.getId());
                    setProperty(fieldName, d.getName());

                    Optional.of(d)
                            .map(Department::getOrganization)
                            .map(Organization::getId)
                            .ifPresent(s -> setProperty(fieldOrgId, s));

                    Optional.of(d)
                            .map(Department::getParent)
                            .map(Department::getId)
                            .ifPresent(s -> setProperty(fieldParentDeptId, s));

                    Optional.of(d)
                            .map(Department::getHod)
                            .map(Employment::getUserId)
                            .ifPresent(s -> setProperty(fieldHod, s));

                }})
                .map(r -> (FormRowSet) new FormRowSet() {{
                    add(r);
                    setMultiRow(false);
                }})
                .orElseGet(FormRowSet::new);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final DepartmentDao departmentDao = (DepartmentDao) applicationContext.getBean("departmentDao");
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final EmploymentDao employmentDao = (EmploymentDao) applicationContext.getBean("employmentDao");
        final EnvironmentVariableDao environmentVariableDao = (EnvironmentVariableDao) applicationContext.getBean("environmentVariableDao");

        final String fieldId = "id";
        final String fieldName = "name";
        final String fieldOrgId = "organizationId";
        final String fieldParentDeptId = "parentId";
        final String fieldHod = "hod";

        return Optional.ofNullable(rowSet)
                .stream()
                .flatMap(Collection::stream)
                .map(r -> {
                    Department department = new Department() {{
                        setId(r.getId());
                        setName(r.getProperty(fieldName));

                        Optional.of(r.getProperty(fieldOrgId))
                                .filter(Predicate.not(String::isEmpty))
                                .map(organizationDao::getOrganization)
                                .ifPresent(this::setOrganization);

                        Optional.of(r.getProperty(fieldParentDeptId))
                                .filter(Predicate.not(String::isEmpty))
                                .map(departmentDao::getDepartment)
                                .ifPresent(this::setParent);
                    }};

                    Optional.of(r.getProperty(fieldHod))
                            .filter(Predicate.not(String::isEmpty))
                            .map(userDao::getUserById)
                            .map(User::getEmployments)
                            .stream()
                            .flatMap(Collection<Employment>::stream)
                            .findFirst()
                            .ifPresent(department::setHod);

                    return department;
                })
                .filter(d -> {
                    if(departmentDao.getDepartment(d.getId()) != null) {
                        return departmentDao.updateDepartment(d);
                    } else {
                        return departmentDao.addDepartment(d);
                    }
                })
                .map(d -> (FormRow) new FormRow() {{
                    setProperty(fieldId, d.getId());
                    setProperty(fieldName, d.getName());

                    Optional.of(d)
                            .map(Department::getOrganization)
                            .map(Organization::getId)
                            .ifPresent(s -> setProperty(fieldOrgId, s));

                    Optional.of(d)
                            .map(Department::getParent)
                            .map(Department::getId)
                            .ifPresent(s -> setProperty(fieldParentDeptId, s));

                    Optional.of(d)
                            .map(Department::getHod)
                            .map(Employment::getUserId)
                            .ifPresent(s -> setProperty(fieldHod, s));

                }})
                .collect(Collectors.toCollection(FormRowSet::new));
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
        return DepartmentDirectoryFormBinder.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }


}
