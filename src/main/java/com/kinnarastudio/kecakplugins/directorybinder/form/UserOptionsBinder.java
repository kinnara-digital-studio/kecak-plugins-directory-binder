/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kinnarastudio.kecakplugins.directorybinder.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.directory.model.*;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author Yonathan
 */
public class UserOptionsBinder extends FormBinder implements FormLoadOptionsBinder,PluginWebSupport, FormAjaxOptionsBinder{
    public final static String LABEL = "Directory User Options Binder";
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
        String useAjax = "";
        if (SecurityUtil.getDataEncryption() != null && SecurityUtil.getNonceGenerator() != null) {
            useAjax = ",{name:'useAjax',label:'@@form.useroptionsbinder.useAjax@@',type:'checkbox',value :'false',options :[{value :'true',label :''}]}";
        }
        Object[] arguments = new Object[]{getClassName(), getClassName(), getClassName(), useAjax};
        String json = AppUtil.readPluginResource(this.getClass().getName(), "/properties/userOptionsBinder.json", arguments, true, "messages/userOptionsBinder");
        return json;
    }

    @Override
    public FormRowSet load(Element elmnt, String string, FormData formData) {
        return this.loadAjaxOptions(null);
    }

    @Override
    public boolean useAjax() {
        return "true".equalsIgnoreCase(getPropertyString("useAjax"));
    }

    @Override
    public FormRowSet loadAjaxOptions(String[] dependencyValues) {
        FormRowSet results = new FormRowSet();
        results.setMultiRow(true);
        ApplicationContext ac = AppUtil.getApplicationContext();
        ExtDirectoryManager directoryManager = (ExtDirectoryManager)ac.getBean("directoryManager");
        String orgId = getPropertyString("orgId");
        if ( orgId == null || orgId.isEmpty()) {
            orgId = WorkflowUtil.getCurrentUserOrgId();
        }
        String deptId = null;
        if (this.getPropertyString("deptId") != null && !this.getPropertyString("deptId").isEmpty()) {
            deptId = this.getPropertyString("deptId");
        }
        String groupId = null;
        if (this.getPropertyString("groupId") != null && !this.getPropertyString("groupId").isEmpty()) {
            groupId = this.getPropertyString("groupId");
        }
        String gradeId = null;
        if (dependencyValues != null && this.getPropertyString("grouping") != null && !this.getPropertyString("grouping").isEmpty()) {
            String value = "";
            if (dependencyValues.length > 0) {
                value = dependencyValues[0];
            }
            if ("org".equals(this.getPropertyString("grouping"))) {
                orgId = value;
            } else if ("dept".equals(this.getPropertyString("grouping"))) {
                deptId = value;
            } else if ("grade".equals(this.getPropertyString("grouping"))) {
                gradeId = value;
            }
        }

        final Collection<User> userList = directoryManager.getUsers(null, orgId.equals("*") ? null : orgId, deptId, gradeId, groupId, null, null, "firstName", false, null, null);
        if ("true".equals(this.getPropertyString("addEmptyOption"))) {
            FormRow emptyRow = new FormRow();
            emptyRow.setProperty("value", "");
            emptyRow.setProperty("label", this.getPropertyString("emptyLabel"));
            emptyRow.setProperty("grouping", "");
            results.add(emptyRow);
        }
        if (userList != null) {
            for (User u : userList) {
                User user = u;
                if (user.getUsername() == null) continue;
                FormRow r = new FormRow();
                String value = null;
                if(this.getPropertyString("columnId").equals("email")){
                    if(user.getEmail()!=null && !user.getEmail().isEmpty()){
                        value = user.getEmail();
                    }else{
                        value = user.getUsername();
                    }
                    LogUtil.info(this.getClass().getName(), "Email: " +value);
                }else if(this.getPropertyString("columnId").equals("firstName")){
                    value = user.getFirstName();
                }else{
                    value = user.getUsername();
                }
                r.setProperty("value", value);
                r.setProperty("label", user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")");
                String grouping = "";
                try {
                    if (this.getPropertyString("grouping") != null && user.getEmployments() != null && !user.getEmployments().isEmpty()) {
                        Employment e = (Employment)user.getEmployments().iterator().next();
                        if ("org".equals(this.getPropertyString("grouping")) && e.getOrganization() != null) {
                            grouping = e.getOrganization().getId();
                        } else if ("dept".equals(this.getPropertyString("grouping")) && e.getDepartment() != null) {
                            grouping = e.getDepartment().getId();
                        } else if ("grade".equals(this.getPropertyString("grouping")) && e.getGrade() != null) {
                            grouping = e.getGrade().getId();
                        }
                        if (grouping == null) {
                            grouping = "";
                        }
                    }
                }
                catch (Exception e) {
                    // empty catch block
                }
                r.setProperty("grouping", grouping);
                results.add(r);
            }
        }
        return results;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUtil.ROLE_ADMIN);
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String action = request.getParameter("action");

        // Get organization options
        if ("getOptions".equals(action)) {
            try {
                JSONArray jsonArray = new JSONArray();
                ApplicationContext ac = AppUtil.getApplicationContext();
                ExtDirectoryManager directoryManager = (ExtDirectoryManager)ac.getBean("directoryManager");
                Collection<Organization> orgList = directoryManager.getOrganizationsByFilter(null, "name", Boolean.valueOf(false), null, null);

                Map<String, String> empty = new HashMap<>();
                empty.put("value", "");
                empty.put("label", "<Empty>");
                jsonArray.put(empty);

                Map<String, String> all = new HashMap<>();
                all.put("value", "*");
                all.put("label", "*");
                jsonArray.put(all);

                for (Organization org : orgList) {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", org.getId());
                    option.put("label", org.getName());
                    jsonArray.put(option);
                }
                jsonArray.write(response.getWriter());
            }
            catch (Exception ex) {
                LogUtil.error(this.getClass().getName(), ex, "Get Organization options Error!");
            }
        } else if ("getDeptOptions".equals(action)) {
            String orgId = request.getParameter("orgId");
            if ("null".equals(orgId) || "".equals(orgId)) {
                orgId = null;
            }
            try {
                ApplicationContext ac = AppUtil.getApplicationContext();
                ExtDirectoryManager directoryManager = (ExtDirectoryManager)ac.getBean("directoryManager");
                Collection<Department> deptList = directoryManager.getDepartmentsByOrganizationId(null, orgId, "name", Boolean.valueOf(false), null, null);

                final JSONArray jsonArray = Optional.ofNullable(deptList)
                        .stream()
                        .flatMap(Collection::stream)
                        .map(Try.onFunction(dept -> {
                            final JSONObject json = new JSONObject();
                            json.put(FormUtil.PROPERTY_VALUE, dept.getId());

                            final String label = (dept.getTreeStructure() != null ? new StringBuilder().append(dept.getTreeStructure()).append(" ").toString() : "") + dept.getName();
                            json.put(FormUtil.PROPERTY_LABEL, label);

                            return json;
                        }))
                        .collect(JSONCollectors.toJSONArray(Try.onSupplier(() -> {
                            final JSONObject empty = new JSONObject();
                            empty.put(FormUtil.PROPERTY_VALUE, "");
                            empty.put(FormUtil.PROPERTY_LABEL, "<Empty>");

                            final JSONArray init = new JSONArray();
                            init.put(empty);

                            return init;
                        })));

                jsonArray.write(response.getWriter());
            }
            catch (Exception ex) {
                LogUtil.error(getClassName(), ex, "Get departments options Error!");
            }
        } else if ("getGroupOptions".equals(action)) {
            String orgId = request.getParameter("orgId");
            if ("null".equals(orgId) || "".equals(orgId)) {
                orgId = null;
            }
            try {
                JSONArray jsonArray = new JSONArray();
                ApplicationContext ac = AppUtil.getApplicationContext();
                ExtDirectoryManager directoryManager = (ExtDirectoryManager)ac.getBean("directoryManager");
                Collection<Group> groupList = directoryManager.getGroupsByOrganizationId(null, orgId, "name", Boolean.valueOf(false), null, null);
                HashMap<String, String> empty = new HashMap<>();
                empty.put("value", "");
                empty.put("label", "");
                jsonArray.put(empty);
                for (Group g : groupList) {
                    HashMap<String, String> option = new HashMap<>();
                    option.put("value", g.getId());
                    option.put("label", g.getName());
                    jsonArray.put(option);
                }


                jsonArray.write(response.getWriter());
            }
            catch (Exception ex) {
                LogUtil.error(this.getClass().getName(), ex, "Get Groups options Error!");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
