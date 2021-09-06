package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.RoleDao;
import org.joget.directory.model.Role;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Stream;

public class RoleDirectoryFormBinder extends FormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder {
    @Override
    public String getFormId() {
        Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_ID);
    }

    @Override
    public String getTableName() {
        Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_TABLE_NAME);
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        final FormRowSet rowSet = new FormRowSet();

        Optional.ofNullable(primaryKey)
                .map(roleDao::getRole)
                .ifPresent(role -> {
                    FormRow roleRow = new FormRow();
                    roleRow.setProperty("id", Optional.ofNullable(role.getId()).orElse(""));
                    roleRow.setProperty("name", Optional.ofNullable(role.getName()).orElse(""));
                    roleRow.setProperty("description", Optional.ofNullable(role.getDescription()).orElse(""));

                    Optional.of(primaryKey)
                            .map(s -> formDataDao.load(getFormId(), getTableName(), s))
                            .map(FormRow::getCustomProperties)
                            .map(Map::entrySet)
                            .map(s -> (Set<Map.Entry<String, Object>>) s)
                            .map(Collection::stream)
                            .orElseGet(Stream::empty)
                            .filter(e -> !roleRow.containsKey(e.getKey()))
                            .forEach(e -> roleRow.setProperty(e.getKey(), String.valueOf(e.getValue())));

                    rowSet.add(roleRow);
                });

        return rowSet;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        final FormRowSet rowSet = new FormRowSet();

        Optional.ofNullable(originalRowSet)
                .map(FormRowSet::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .ifPresent(row -> {
                    Date now = new Date();
                    String currentUser = WorkflowUtil.getCurrentUsername();
                    String primaryKey = row.getId();
                    Role role = roleDao.getRole(primaryKey);

                    if(role == null) {
                        role = new Role();
                        role.setId(row.getId());
                        role.setName(row.getProperty("name"));
                        role.setDescription(row.getProperty("description"));
                        role.setDeleted(false);
                        roleDao.addRole(role);

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    } else {
                        role.setName(row.getProperty("name"));
                        role.setDescription(row.getProperty("description"));
                        role.setDateModified(row.getDateModified());
                        role.setModifiedBy(row.getModifiedBy());
                        roleDao.updateRole(role);
                    }

                    row.setId(role.getId());
                    row.setDateModified(now);
                    row.setModifiedBy(currentUser);
                    row.setDateCreated(role.getDateCreated());
                    row.setCreatedBy(role.getCreatedBy());

                    row.remove("name");
                    row.remove("description");

                    rowSet.add(row);
                    formDataDao.saveOrUpdate(getFormId(), getTableName(), rowSet);
                });

        return rowSet;
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
        return "Role Directory Form Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
}
