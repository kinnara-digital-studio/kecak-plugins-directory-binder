package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.directory.dao.OrganizationDao;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrganizationDirectoryDataListBinder extends FormRowDataListBinder {

    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        final Optional<Form> optForm = Optional.ofNullable(getSelectedForm());
        final Set<String> ids = new HashSet<>();
        final DataListCollection<Map<String, Object>> collect = Optional.ofNullable(organizationDao.findOrganizations(getCondition(map), null, sort, desc, start, rows))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(org -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", org.getId());
                    record.put("name", org.getName());
                    record.put("description", org.getDescription());
                    record.put("parentId", org.getParentId());
                    record.put("dateCreated", org.getDateCreated());
                    record.put("dateModified", org.getDateModified());
                    record.put("createdBy", org.getCreatedBy());
                    record.put("modifiedBy", org.getModifiedBy());

                    // fill form data
                    optForm.map(f -> formDataDao.load(f, org.getId()))
                            .map(FormRow::getCustomProperties)
                            .map(o -> (Map<String, Object>)o)
                            .map(Map::entrySet)
                            .map(Collection::stream)
                            .orElseGet(Stream::empty)
                            .filter(e -> !record.containsKey(e.getKey()))
                            .forEach(e -> record.put(e.getKey(), e.getValue()));

                    return record;
                })
                .collect(Collectors.toCollection(DataListCollection::new));

        return collect;
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        return Math.toIntExact(organizationDao.countOrganizations(getCondition(map), null));
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

    protected String getCondition(Map properties) {
        return Optional.of("extraCondition")
                .map(properties::get)
                .map(String::valueOf)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> "where " + s)
                .orElse("");
    }
}
