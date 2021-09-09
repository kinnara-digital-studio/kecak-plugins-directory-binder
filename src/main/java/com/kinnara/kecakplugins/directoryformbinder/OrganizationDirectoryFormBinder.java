package com.kinnara.kecakplugins.directoryformbinder;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.model.Organization;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class OrganizationDirectoryFormBinder extends DefaultFormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder {
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
    	FormRowSet table = super.load(element, primaryKey, formData);
    	
    	if(table!=null && table.size()>0) {
    		return table;
    	}else {
    		final ApplicationContext applicationContext = AppUtil.getApplicationContext();
            final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
            final FormRowSet results = new FormRowSet();
            Optional.ofNullable(primaryKey)
            .map(organizationDao::getOrganization)
            .ifPresent(org -> {
                FormRow row = new FormRow();
    			row.setId(org.getId());
                row.setProperty("name", Optional.ofNullable(org.getName()).orElse(""));
                row.setProperty("description", Optional.ofNullable(org.getDescription()).orElse(""));
                row.setProperty("parentId", Optional.ofNullable(org.getParentId()).orElse(""));

                results.add(row);
            });
            return results;
    	}
    }

    @Override
    public FormRowSet store(Element element, FormRowSet formRowSet, FormData formData) {
    	final ApplicationContext applicationContext = AppUtil.getApplicationContext();
    	final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        Optional.ofNullable(formRowSet)
                .map(FormRowSet::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .ifPresent(row -> {
                    final String primaryKey = Optional.of(row)
                            .map(FormRow::getId)
                            .orElseGet(formData::getPrimaryKeyValue);

                    @Nullable Organization org = Optional.ofNullable(primaryKey)
                            .map(organizationDao::getOrganization)
                            .orElse(null);

                    if (org == null) {
                    	org = new Organization();
                    	if(row.getId()!=null && !row.getId().equals("")) {
                    		org.setId(row.getId());
                    	}else {
                    		UuidGenerator uuid = UuidGenerator.getInstance();
                    		org.setId(uuid.getUuid());
                    	}
                    	org.setName(row.getProperty("name"));
                    	org.setDescription(row.getProperty("description"));
                    	if(row.getProperty("parentId")!=null && !row.getProperty("parentId").equals(""))
                    		org.setParentId(row.getProperty("parentId"));
                        org.setDateCreated(now);
                        org.setCreatedBy(currentUser);
                        organizationDao.addOrganization(org);

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    } else {
                    	org.setName(row.getProperty("name"));
                    	org.setDescription(row.getProperty("description"));
                    	if(row.getProperty("parentId")!=null && !row.getProperty("parentId").equals(""))
                    		org.setParentId(row.getProperty("parentId"));
                        org.setDateModified(row.getDateModified());
                        org.setModifiedBy(row.getModifiedBy());
                        organizationDao.updateOrganization(org);
                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    }
                });
        formRowSet = super.store(element, formRowSet, formData);
        
        return formRowSet;
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
        return "Organization Directory Form Binder";
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
