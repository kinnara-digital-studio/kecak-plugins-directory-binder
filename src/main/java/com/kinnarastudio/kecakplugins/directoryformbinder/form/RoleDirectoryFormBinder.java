package com.kinnarastudio.kecakplugins.directoryformbinder.form;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.directory.dao.RoleDao;
import org.joget.directory.model.Role;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 *
 */
public class RoleDirectoryFormBinder extends FormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder {
    public final static String LABEL = "Role Directory Form Binder";

    @Override
    public String getFormId() {
        Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_ID);
    }

    @Override
    public String getTableName() {
        return "dir_role";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");

        final FormRowSet rowSet = new FormRowSet();

        Optional.ofNullable(primaryKey)
                .map(roleDao::getRole)
                .ifPresent(role -> {
                    FormRow roleRow = new FormRow();
                    roleRow.setProperty("id", Optional.ofNullable(role.getId()).orElse(""));
                    roleRow.setProperty("name", Optional.ofNullable(role.getName()).orElse(""));
                    roleRow.setProperty("description", Optional.ofNullable(role.getDescription()).orElse(""));
                    rowSet.add(roleRow);
                });

        return rowSet;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        Optional.ofNullable(originalRowSet)
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .ifPresent(row -> {
                    final String primaryKey = Optional.of(row)
                            .map(FormRow::getId)
                            .orElseGet(formData::getPrimaryKeyValue);

                    @Nullable Role role = Optional.ofNullable(primaryKey)
                            .map(roleDao::getRole)
                            .orElse(null);

                    if(role == null) {
                        role = new Role();
                        role.setId(row.getId());
                        role.setName(row.getProperty("name"));
                        role.setDescription(row.getProperty("description"));
                        roleDao.addRole(role);

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    } else {
                        role.setName(row.getProperty("name"));
                        role.setDescription(row.getProperty("description"));
                        roleDao.updateRole(role);
                    }

                    row.setId(role.getId());
                });

        return originalRowSet;
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
}
