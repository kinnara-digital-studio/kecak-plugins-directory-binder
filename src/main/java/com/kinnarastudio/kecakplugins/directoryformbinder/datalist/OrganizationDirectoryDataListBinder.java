package com.kinnarastudio.kecakplugins.directoryformbinder.datalist;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.directory.dao.OrganizationDao;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrganizationDirectoryDataListBinder extends FormRowDataListBinder {
    public final static String LABEL = "Organization Directory DataList Binder";

    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] filter, String sort, Boolean desc, Integer start, Integer rows) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
        final Optional<Form> optForm = Optional.ofNullable(getSelectedForm());
        final DataListFilterQueryObject criteria = getCriteria(map, filter);
        final DataListCollection<Map<String, Object>> collect = Optional.ofNullable(organizationDao.findOrganizations(criteria.getQuery(), criteria.getValues(), sort, desc, start, rows))
                .stream()
                .flatMap(Collection::stream)
                .map(org -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", org.getId());
                    record.put("name", org.getName());
                    record.put("description", org.getDescription());
                    record.put("parentId", org.getParentId());

                    // fill form data
                    optForm.map(f -> formDataDao.load(f, org.getId()))
                            .map(FormRow::getCustomProperties)
                            .map(o -> (Map<String, Object>) o)
                            .map(Map::entrySet)
                            .stream()
                            .flatMap(Collection::stream)
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
        final DataListFilterQueryObject criteria = getCriteria(map, filter);
        return Math.toIntExact(organizationDao.countOrganizations(criteria.getQuery(), criteria.getValues()));
    }

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

    /**
     *
     * @param map
     * @param filter
     * @return
     */
    protected DataListFilterQueryObject getOrganizationCriteria(Map map, DataListFilterQueryObject[] filter) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
        final DataListFilterQueryObject criteria = getCriteria(map, filter);
        final Optional<Form> optForm = Optional.ofNullable(getSelectedForm());

        final Set<String> cachedIds = optForm
                .map(f -> formDataDao.find(f, criteria.getQuery(), criteria.getValues(), null, null, null, null))
                .stream()
                .flatMap(Collection::stream)
                .map(FormRow::getId)
                .collect(Collectors.toSet());

        final DataListFilterQueryObject orgCriteria = new DataListFilterQueryObject();
        orgCriteria.setOperator("AND");

        final String condition = cachedIds.isEmpty() ? null : ("where id in (" + cachedIds.stream().map(s -> "?").collect(Collectors.joining(", ")) + ")");
        orgCriteria.setQuery(condition);

        final String[] parameters = cachedIds.toArray(new String[0]);
        orgCriteria.setValues(parameters);

        return orgCriteria;
    }

    @Override
    protected DataListFilterQueryObject getCriteria(Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        final DataListFilterQueryObject criteria = super.getCriteria(properties, filterQueryObjects);
        criteria.setQuery(criteria.getQuery().replaceAll("customProperties", "e"));
        return criteria;
    }
}
