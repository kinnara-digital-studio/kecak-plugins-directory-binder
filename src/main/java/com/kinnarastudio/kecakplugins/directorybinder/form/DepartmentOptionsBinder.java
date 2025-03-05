package com.kinnarastudio.kecakplugins.directorybinder.form;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.Department;
import org.joget.directory.model.Organization;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class DepartmentOptionsBinder extends FormBinder implements FormLoadOptionsBinder, PluginWebSupport {
    @Override
    public String getName() {
        return AppPluginUtil.getMessage("form.departmentOptionsBinder.title", getClassName(), "/messages/departmentOptionsBinder");
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
        Object[] arguments = new Object[]{getClassName()};
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/departmentOptionsBinder.json", arguments, true, "messages/departmentOptionsBinder");
        return json;
    }

    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet results = new FormRowSet();
        results.setMultiRow(true);

        String orgId = getPropertyString("orgId");
        if (orgId == null || orgId.isEmpty()) {
            orgId = WorkflowUtil.getCurrentUserOrgId();
        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        ExtDirectoryManager directoryManager = (ExtDirectoryManager) appContext.getBean("directoryManager");
        Collection<Department> deptList = directoryManager.getDepartmentsByOrganizationId(null, orgId.equals("*") ? null : orgId, "name", Boolean.valueOf(false), null, null);
        if(deptList == null)
            deptList = new ArrayList<>();

        if ("true".equals(this.getPropertyString("addEmptyOption"))) {
            FormRow emptyRow = new FormRow();
            emptyRow.setProperty("value", "");
            emptyRow.setProperty("label", this.getPropertyString("emptyLabel"));
            emptyRow.setProperty("grouping", "");
            results.add(emptyRow);
        }

        for (Department d : deptList) {
            if (d.getId() == null) {
                continue;
            }

            FormRow row = new FormRow();
            row.setProperty("value", d.getId());
            row.setProperty("label", (d.getTreeStructure() != null ? d.getTreeStructure() + " " : "") + d.getName());
            String grouping = "";
            if (d.getOrganization() != null) {
                grouping = d.getOrganization().getId();
            }
            row.setProperty("grouping", grouping);
            results.add(row);
        }

        return results;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUtil.ROLE_ADMIN);
            if (!isAdmin) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String action = request.getParameter("action");
            if ("getOptions".equals(action)) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    ApplicationContext ac = AppUtil.getApplicationContext();
                    ExtDirectoryManager directoryManager = (ExtDirectoryManager) ac.getBean("directoryManager");
                    Collection<Organization> orgList = directoryManager.getOrganizationsByFilter(null, "name", false, null, null);

                    final Map<String, String> empty = new HashMap<>();
                    empty.put(FormUtil.PROPERTY_VALUE, "");
                    empty.put(FormUtil.PROPERTY_LABEL, "");
                    jsonArray.put(empty);

                    final Map<String, String> all = new HashMap<>();
                    all.put(FormUtil.PROPERTY_VALUE, "*");
                    all.put(FormUtil.PROPERTY_LABEL, "*");
                    jsonArray.put(all);

                    for (Organization o : orgList) {
                        Map<String, String> option = new HashMap<>();
                        option.put("value", o.getId());
                        option.put(FormUtil.PROPERTY_LABEL, o.getName());
                        jsonArray.put(option);
                    }
                    jsonArray.write(response.getWriter());
                } catch (BeansException | IOException ex) {
                    LogUtil.error(getClassName(), ex, "Get Organization options Error!");
                } catch (JSONException ex) {
                    LogUtil.error(getClassName(), ex,ex.getMessage());
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

}
