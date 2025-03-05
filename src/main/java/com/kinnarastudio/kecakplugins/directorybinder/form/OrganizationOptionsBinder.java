package com.kinnarastudio.kecakplugins.directorybinder.form;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OrganizationOptionsBinder extends FormBinder implements FormLoadOptionsBinder,FormAjaxOptionsBinder {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        ExtDirectoryManager directoryManager = (ExtDirectoryManager) appContext.getBean("directoryManager");

        // retrieve data
        FormRowSet results =  Optional.ofNullable(directoryManager.getOrganizationsByFilter(null, "name", false, null, null))
                .stream()
                .flatMap(Collection::stream)
                .map(o -> {
                    FormRow row = new FormRow();
                    row.setProperty("value", o.getId());
                    row.setProperty("label", o.getName());
                    return row;
                })
                .collect(Collectors.toCollection(FormRowSet::new));

        // Empty Option
        if ("true".equals(getPropertyString("addEmptyOption"))) {
            FormRow emptyRow = new FormRow();
            emptyRow.setProperty("value", "");
            emptyRow.setProperty("label", getPropertyString("emptyLabel"));
            emptyRow.setProperty("grouping", "");
            results.add(0, emptyRow);
        }

        return results;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("org.optionsbinder.title", getClassName(), "/messages/organizationOptionaBinder");
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
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/organizationOptionsBinder.json", null, true, "messages/organizationOptionaBinder");
        return json;
    }

	@Override
	public boolean useAjax() {
		return "true".equalsIgnoreCase(getPropertyString("useAjax"));
	}

	@Override
	public FormRowSet loadAjaxOptions(String[] dependencyValues) {
		return load(null, null, getFormData());
	}
}
