package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.*;
import org.joget.directory.dao.RoleDao;
import org.joget.directory.dao.UserDao;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserDirectoryDataListBinder extends FormRowDataListBinder {
    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        return Optional.ofNullable(userDao.getUsers(null, null, null, null, null, null, null, sort, desc, start, rows))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(r -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", r.getId());
                    record.put("username", r.getUsername());
                    record.put("firstName", r.getFirstName());
                    record.put("email", r.getEmail());
                    record.put("timeZone", r.getTimeZone());
                    record.put("telephone_number", r.getTelephoneNumber());
                    record.put("active", r.getActive());
                    record.put("dateCreated", r.getDateCreated());
                    record.put("dateModified", r.getDateModified());
                    record.put("createdBy", r.getCreatedBy());
                    record.put("modifiedBy", r.getModifiedBy());
                    return record;
                })
                .collect(Collectors.toCollection(DataListCollection::new));
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        return Math.toIntExact(userDao.getTotalUsers(null, null, null, null, null, null, null));
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
        return "User Directory DataList Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }
}
