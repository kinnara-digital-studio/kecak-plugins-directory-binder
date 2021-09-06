package com.kinnara.kecakplugins.directoryformbinder;

import java.util.ArrayList;
import java.util.Collection;
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

        registrationList.add(context.registerService(RoleDirectoryDataListBinder.class.getName(), new RoleDirectoryDataListBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}