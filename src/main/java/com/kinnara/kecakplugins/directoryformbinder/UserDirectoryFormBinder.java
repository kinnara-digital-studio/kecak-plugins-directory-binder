package com.kinnara.kecakplugins.directoryformbinder;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class UserDirectoryFormBinder extends FormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder  {
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
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final FormRowSet results = new FormRowSet();
        
        Optional.ofNullable(primaryKey)
        .map(userDao::getUserById)
        .ifPresent(user -> {
        	FormRow row = new FormRow();
        	row.setProperty("username", Optional.ofNullable(user.getUsername()).orElse(""));
        	row.setProperty("firstName", Optional.ofNullable(user.getFirstName()).orElse(""));
        	row.setProperty("lastName", Optional.ofNullable(user.getLastName()).orElse(""));
        	row.setProperty("email", Optional.ofNullable(user.getEmail()).orElse(""));
        	String active = "true";
        	if(user.getActive()!=1) {
        		active = "false";
        	}
        	row.setProperty("active", active);
        	row.setProperty("locale", Optional.ofNullable(user.getLocale()).orElse(""));
        	row.setProperty("telephone_number", Optional.ofNullable(user.getTelephoneNumber()).orElse(""));
        	results.add(row);
        });
        return results;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet formRowSet, FormData formData) {
    	final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

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

                    @Nullable User user = Optional.ofNullable(primaryKey)
                            .map(userDao::getUserById)
                            .orElse(null);

                    if(user == null) {
                    	user = new User();
                    	user.setId(row.getId());
                    	user.setUsername(row.getProperty("username"));
                    	user.setFirstName(row.getProperty("firstName"));
                    	user.setLastName(row.getProperty("lastName"));
                    	user.setEmail(row.getProperty("email"));
                    	if(row.getProperty("active").equals("true")
                    			||row.getProperty("active").equals("active")
                    			||row.getProperty("active").equals("1")) {
                    		user.setActive(1);
                    	}else {
                    		user.setActive(0);
                    	}
                    	user.setLocale(row.getProperty("locale"));
                    	user.setTelephoneNumber(row.getProperty("telephoneNumber"));
                    	
                        userDao.addUser(user);

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    } else {
                    	user.setUsername(row.getProperty("username"));
                    	user.setFirstName(row.getProperty("firstName"));
                    	user.setLastName(row.getProperty("lastName"));
                    	user.setEmail(row.getProperty("email"));
                    	if(row.getProperty("active").equals("true")
                    			||row.getProperty("active").equals("active")
                    			||row.getProperty("active").equals("1")) {
                    		user.setActive(1);
                    	}else {
                    		user.setActive(0);
                    	}
                    	user.setLocale(row.getProperty("locale"));
                    	user.setTelephoneNumber(row.getProperty("telephoneNumber"));
                    	user.setDateModified(row.getDateModified());
                    	user.setModifiedBy(row.getModifiedBy());
                        userDao.updateUser(user);
                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    }
                });

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
        return "User Directory Form Binder";
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
