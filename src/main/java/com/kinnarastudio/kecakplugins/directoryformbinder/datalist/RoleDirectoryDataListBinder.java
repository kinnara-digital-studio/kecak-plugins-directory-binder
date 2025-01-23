package com.kinnarastudio.kecakplugins.directoryformbinder.datalist;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.directory.dao.RoleDao;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

public class RoleDirectoryDataListBinder extends FormRowDataListBinder {
    public final static String LABEL = "Role Directory DataList Binder";

    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        return Optional.ofNullable(roleDao.getRoles(Optional.of("extraCondition")
                        .map(map::get)
                        .map(String::valueOf)
                        .orElse(""), sort, desc, start, rows))
                .stream().flatMap(Collection::stream)
                .map(r -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", r.getId());
                    record.put("name", r.getName());
                    record.put("description", r.getDescription());
                    return record;
                })
                .collect(Collectors.toCollection(DataListCollection::new));
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        return Math.toIntExact(roleDao.getTotalRoles(Optional.of("extraCondition").map(map::get).map(String::valueOf).orElse("")));
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
}
