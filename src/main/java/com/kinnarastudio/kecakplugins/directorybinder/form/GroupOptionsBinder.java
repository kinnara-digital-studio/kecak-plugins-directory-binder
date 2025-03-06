package com.kinnarastudio.kecakplugins.directorybinder.form;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.Group;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yonathan
 */
public class GroupOptionsBinder extends FormBinder implements FormLoadOptionsBinder, PluginWebSupport {

    public String getName() {
        return AppPluginUtil.getMessage("group.optionsbinder.title", getClassName(), "/messages/groupOptionsBinder");
    }

    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    public String getLabel() {
        return getName();
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        Object[] arguments = new Object[]{getClassName()};
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/groupOptionsBinder.json", arguments, true, "messages/groupOptionsBinder");
        return json;
    }

    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet results = new FormRowSet();
        results.setMultiRow(true);

        String orgId = this.getPropertyString("orgId");
        if ( orgId == null || orgId.isEmpty()) {
            orgId = WorkflowUtil.getCurrentUserOrgId();
        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        ExtDirectoryManager directoryManager = (ExtDirectoryManager) appContext.getBean("directoryManager");
        Collection<Group> groups = directoryManager.getGroupsByOrganizationId(null, orgId.equals("*") ? null : orgId, "name", false, null, null);
        if ("true".equals(this.getPropertyString("addEmptyOption"))) {
            FormRow emptyRow = new FormRow();
            emptyRow.setProperty("value", "");
            emptyRow.setProperty("label", this.getPropertyString("emptyLabel"));
            emptyRow.setProperty("grouping", "");
            results.add(emptyRow);
        }
        if (groups != null) {
            for (Group g : groups) {
                if (g.getId() == null) {
                    continue;
                }
                FormRow row = new FormRow();
                row.setProperty("value", g.getId());
                row.setProperty("label", g.getName() != null ? g.getName() : "");
                String grouping = "";
                if (g.getOrganization() != null) {
                    grouping = g.getOrganization().getId();
                }
                row.setProperty("grouping", grouping);
                results.add(row);
            }
        }
        return results;
    }

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            boolean isAdmin = WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN");
            if (!isAdmin) {
                response.sendError(401);
                return;
            }
            String action = request.getParameter("action");
            if ("getOptions".equals(action)) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    ApplicationContext ac = AppUtil.getApplicationContext();
                    ExtDirectoryManager directoryManager = (ExtDirectoryManager) ac.getBean("directoryManager");
                    Collection<Organization> orgList = directoryManager.getOrganizationsByFilter(null, "name", false, null, null);

                    Map<String, String> empty = new HashMap<String, String>();
                    empty.put("value", "");
                    empty.put("label", "");
                    jsonArray.put(empty);

                    Map<String, String> all = new HashMap<>();
                    all.put("value", "*");
                    all.put("label", "*");
                    jsonArray.put(all);

                    for (Organization o : orgList) {
                        Map<String, String> option = new HashMap<String, String>();
                        option.put("value", o.getId());
                        option.put("label", o.getName());
                        jsonArray.put(option);
                    }
                    jsonArray.write(response.getWriter());
                } catch (BeansException | IOException ex) {
                    LogUtil.error(this.getClass().getName(), ex, "Get Organization options Error!");
                } catch (JSONException ex) {
                    Logger.getLogger(GroupOptionsBinder.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                response.setStatus(204);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

}
