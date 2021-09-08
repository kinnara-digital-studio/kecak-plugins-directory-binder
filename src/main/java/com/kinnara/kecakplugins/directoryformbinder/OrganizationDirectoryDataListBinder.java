package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.*;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.dao.RoleDao;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrganizationDirectoryDataListBinder extends FormRowDataListBinder {
    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        return Optional.ofNullable(organizationDao.getOrganizationsByFilter(Optional.of("extraCondition").map(map::get).map(String::valueOf).orElse(""), sort, desc, start, rows))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(r -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", r.getId());
                    record.put("name", r.getName());
                    record.put("description", r.getDescription());
                    record.put("parentId", r.getParentId());
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
        OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        return Math.toIntExact(organizationDao.getTotalOrganizationsByFilter(Optional.of("extraCondition").map(map::get).map(String::valueOf).orElse("")));
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
        return "Organization Directory DataList Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }
}
