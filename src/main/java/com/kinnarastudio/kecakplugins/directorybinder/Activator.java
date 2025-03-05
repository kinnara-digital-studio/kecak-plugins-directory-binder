package com.kinnarastudio.kecakplugins.directorybinder;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.directorybinder.datalist.OrganizationDirectoryDataListBinder;
import com.kinnarastudio.kecakplugins.directorybinder.datalist.RoleDirectoryDataListBinder;
import com.kinnarastudio.kecakplugins.directorybinder.datalist.UserDirectoryDataListBinder;
import com.kinnarastudio.kecakplugins.directorybinder.form.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(UserDirectoryFormBinder.class.getName(), new UserDirectoryFormBinder(), null));
        registrationList.add(context.registerService(OrganizationDirectoryFormBinder.class.getName(), new OrganizationDirectoryFormBinder(), null));
        registrationList.add(context.registerService(RoleDirectoryFormBinder.class.getName(), new RoleDirectoryFormBinder(), null));

        // datalist binder
        registrationList.add(context.registerService(RoleDirectoryDataListBinder.class.getName(), new RoleDirectoryDataListBinder(), null));
        registrationList.add(context.registerService(UserDirectoryDataListBinder.class.getName(), new UserDirectoryDataListBinder(), null));

        // form binder
        registrationList.add(context.registerService(DepartmentDirectoryFormBinder.class.getName(), new DepartmentDirectoryFormBinder(), null));
        registrationList.add(context.registerService(OrganizationDirectoryDataListBinder.class.getName(), new OrganizationDirectoryDataListBinder(), null));

        // options binder
        registrationList.add(context.registerService(DepartmentOptionsBinder.class.getName(), new DepartmentOptionsBinder(), null));
        registrationList.add(context.registerService(EmploymentOptionsBinder.class.getName(), new EmploymentOptionsBinder(), null));
        registrationList.add(context.registerService(GroupOptionsBinder.class.getName(), new GroupOptionsBinder(), null));
        registrationList.add(context.registerService(OrganizationOptionsBinder.class.getName(), new OrganizationOptionsBinder(), null));
        registrationList.add(context.registerService(UserOptionsBinder.class.getName(), new UserOptionsBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}