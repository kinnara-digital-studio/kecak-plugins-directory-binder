package com.kinnarastudio.kecakplugins.directorybinder.form;

import java.util.ResourceBundle;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormLoadElementBinder;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;
import org.joget.apps.form.model.FormStoreElementBinder;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

public class GroupDirectoryFormBinder extends FormBinder implements FormLoadBinder, FormStoreBinder, FormLoadElementBinder,
        FormStoreElementBinder {

    private static final String LABEL = "Group Directory Form Binder";

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/form/GroupDirectoryFormBinder.json");
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        return resourceBundle.getString("buildNumber");
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
    if (rowSet != null && !rowSet.isEmpty()) {

        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");

        String formId = getPropertyString("formId");
        String tableName = getPropertyString("tableName");

        formDataDao.saveOrUpdate(formId, tableName, rowSet);

        return rowSet;
    }
    return new FormRowSet();
}

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'load'");
    }

}
