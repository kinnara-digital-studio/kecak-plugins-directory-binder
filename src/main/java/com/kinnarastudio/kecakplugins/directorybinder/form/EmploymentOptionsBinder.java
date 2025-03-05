package com.kinnarastudio.kecakplugins.directorybinder.form;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ResourceBundle;

public class EmploymentOptionsBinder extends FormBinder implements FormLoadOptionsBinder, PluginWebSupport {
    public final static String LABEL = "Employment Options Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        ExtDirectoryManager directoryManager = (ExtDirectoryManager) appContext.getBean("directoryManager");

        return null;
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
    public void webService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return EmploymentOptionsBinder.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
}
