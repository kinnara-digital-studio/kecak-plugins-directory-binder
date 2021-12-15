package com.kinnara.kecakplugins.directoryformbinder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.OrganizationDao;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrganizationDirectoryDataListBinder extends FormRowDataListBinder {
    final Set<String> cachedIds = new HashSet<>();

    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] filter, String sort, Boolean desc, Integer start, Integer rows) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        final Optional<Form> optForm = Optional.ofNullable(getSelectedForm());
        final DataListFilterQueryObject orgCriteria = getOrganizationCriteria(map, filter);

        final DataListCollection<Map<String, Object>> collect = Optional.ofNullable(organizationDao.findOrganizations(orgCriteria.getQuery(), orgCriteria.getValues(), sort, desc, start, rows))
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
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] filter) {
        final OrganizationDao organizationDao = (OrganizationDao) AppUtil.getApplicationContext().getBean("organizationDao");
        final DataListFilterQueryObject orgCriteria = getOrganizationCriteria(map, filter);
        return Math.toIntExact(organizationDao.countOrganizations(orgCriteria.getQuery(), orgCriteria.getValues()));
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

    /**
     *
     * @param map
     * @param filter
     * @return
     */
    protected DataListFilterQueryObject getOrganizationCriteria(Map map, DataListFilterQueryObject[] filter) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
        final DataListFilterQueryObject formCriteria = getCriteria(map, filter);

        final Optional<Form> optForm = Optional.ofNullable(getSelectedForm());
        if(cachedIds.isEmpty()) {
            cachedIds.addAll(optForm
                    .map(f -> formDataDao.find(f, formCriteria.getQuery(), formCriteria.getValues(), null, null, null, null))
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .map(FormRow::getId)
                    .collect(Collectors.toSet()));
        }

        final String condition = cachedIds.isEmpty() ? null : ("where id in (" + cachedIds.stream().map(s -> "?").collect(Collectors.joining(", ")) + ")");
        final String[] parameters = cachedIds.toArray(new String[0]);

        final DataListFilterQueryObject orgCriteria = new DataListFilterQueryObject();
        orgCriteria.setOperator("AND");
        orgCriteria.setQuery(condition);
        orgCriteria.setValues(parameters);
        return orgCriteria;
    }
}
