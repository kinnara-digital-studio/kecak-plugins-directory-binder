package com.kinnarastudio.kecakplugins.directorybinder.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.directory.dao.UserDao;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserDirectoryDataListBinder extends FormRowDataListBinder {
    public final static String LABEL = "User Directory DataList Binder";

    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final DataListFilterQueryObject criteria = getCriteria(map, filterQueryObjects);
        return Optional.ofNullable(userDao.findUsers(criteria.getQuery(), criteria.getValues(), sort, desc, start, rows))
                .stream()
                .flatMap(Collection::stream)
                .map(r -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", r.getId());
                    record.put("username", r.getUsername());
                    record.put("firstName", r.getFirstName());
                    record.put("lastName", r.getLastName());
                    record.put("email", r.getEmail());
                    record.put("timeZone", r.getTimeZone());
                    record.put("telephone_number", r.getTelephoneNumber());
                    record.put("active", r.getActive());
                    return record;
                })
                .collect(Collectors.toCollection(DataListCollection::new));
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] filterQueryObjects) {
        final UserDao userDao = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        final DataListFilterQueryObject criteria = getCriteria(map, filterQueryObjects);
        return Math.toIntExact(userDao.countUsers(criteria.getQuery(), criteria.getValues()));
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

    @Override
    public String getPropertyOptions() {

        Stream<JSONObject> stream1 = Optional.ofNullable(super.getPropertyOptions())
                .map(Try.onFunction(JSONArray::new))
                .map(json -> JSONStream.of(json, Try.onBiFunction(JSONArray::getJSONObject)))
                .orElseGet(Stream::empty);

        String str = AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/UserDirectoryDataListBinder.json");
        Stream<JSONObject> stream2 = Optional.of(str)
                .map(Try.onFunction(JSONArray::new))
                .map(json -> JSONStream.of(json, Try.onBiFunction(JSONArray::getJSONObject)))
                .orElseGet(Stream::empty);

        return Stream.concat(stream1, stream2).collect(JSONCollectors.toJSONArray()).toString();
    }

    protected boolean isHideAdminRole() {
        return "true".equalsIgnoreCase(getPropertyString("hideAdminRole"));
    }

    protected String filter(DataListFilterQueryObject[] filters) {
        return Optional.ofNullable(filters)
                .stream()
                .flatMap(Arrays::stream)
                .map(DataListFilterQueryObject::getValues)
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("");

    }

    @Override
    protected DataListFilterQueryObject getCriteria(Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        final DataListFilterQueryObject criteria = super.getCriteria(properties, filterQueryObjects);
        criteria.setQuery(criteria.getQuery().replaceAll("customProperties", "e"));

        if(isHideAdminRole()) {
            final String query = criteria.getQuery();
            criteria.setQuery(query + (query.isEmpty() ? " where " : " and ") + " e.id not in (select u.id from User u join u.roles r where r.id = ?)");

            final List<String> values = Optional.of(criteria)
                    .map(DataListFilterQueryObject::getValues)
                    .stream()
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());

            values.add(WorkflowUtil.ROLE_ADMIN);
            criteria.setValues(values.toArray(new String[0]));
        }

        return criteria;
    }
}
