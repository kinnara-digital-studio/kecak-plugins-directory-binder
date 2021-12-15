package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.model.Organization;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

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
        final FormRowSet formRowSet = super.load(element, primaryKey, formData);
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");

        final FormRowSet result = Optional.ofNullable(primaryKey)
                .map(organizationDao::getOrganization)
                .map(org -> {
                    final FormRow row = Optional.ofNullable(formRowSet)
                            .map(Collection::stream)
                            .orElseGet(Stream::empty)
                            .findFirst()
                            .orElseGet(FormRow::new);

                    row.setId(org.getId());
                    row.setProperty("name", Optional.ofNullable(org.getName()).orElse(""));
                    row.setProperty("description", Optional.ofNullable(org.getDescription()).orElse(""));
                    row.setProperty("parentId", Optional.ofNullable(org.getParentId()).orElse(""));

                    FormRowSet rowSet = new FormRowSet();
                    rowSet.add(row);
                    return rowSet;
                })
                .orElseGet(FormRowSet::new);
        return result;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        // insert or update to table dir_org
        final FormRowSet savedRowSet = Optional.ofNullable(originalRowSet)
                .map(FormRowSet::stream)
                .orElseGet(Stream::empty)

                // assume this is not multirow
                .findFirst()

                .map(row -> {
                    // load data from dir_org
                    final Optional<Organization> optOrganization = Optional.of(row)
                            .map(FormRow::getId)
                            .map(organizationDao::getOrganization);

                    if (optOrganization.isPresent()) {
                        final Organization org = optOrganization.get();
                        org.setName(row.getProperty("name"));
                        org.setDescription(row.getProperty("description"));
                        Optional.ofNullable(row.getProperty("parentId")).filter(s -> !s.isEmpty()).ifPresent(org::setParentId);
                        org.setDateModified(row.getDateModified());
                        org.setModifiedBy(row.getModifiedBy());
                        organizationDao.updateOrganization(org);
                    } else {
                        final Organization org = new Organization();
                        final String orgId = row.getId();
                        if (orgId != null && !orgId.isEmpty()) {
                            org.setId(orgId);
                        } else {
                            final UuidGenerator uuid = UuidGenerator.getInstance();
                            org.setId(uuid.getUuid());
                        }

                        org.setName(row.getProperty("name"));
                        org.setDescription(row.getProperty("description"));
                        Optional.ofNullable(row.getProperty("parentId")).filter(s -> !s.isEmpty()).ifPresent(org::setParentId);
                        org.setDateCreated(now);
                        org.setCreatedBy(currentUser);
                        organizationDao.addOrganization(org);

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    }

                    row.setDateModified(now);
                    row.setModifiedBy(currentUser);

                    final FormRowSet rowSet = new FormRowSet();
                    rowSet.add(row);
                    return rowSet;

                }).orElse(originalRowSet);

        // // insert or update to form table
        return super.store(element, savedRowSet, formData);
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
