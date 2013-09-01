package org.erlide.model;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Plugin;
import org.erlide.engine.ErlangEngine;
import org.erlide.runtime.api.IRpcSite;
import org.erlide.runtime.api.IRpcSiteProvider;
import org.erlide.runtime.runtimeinfo.RuntimeVersion;
import org.erlide.util.services.ExtensionUtils;
import org.osgi.framework.BundleContext;

public class ModelPlugin extends Plugin {

    public static final String PLUGIN_ID = "org.erlide.model";
    private static final String CORE_PLUGIN_ID = "org.erlide.core";
    public static final String NATURE_ID = CORE_PLUGIN_ID + ".erlnature";
    public static final String BUILDER_ID = CORE_PLUGIN_ID + ".erlbuilder";

    private static BundleContext context;
    static ModelPlugin plugin;

    public ModelPlugin() {
        super();
        plugin = this;
    }

    public static ModelPlugin getDefault() {
        if (plugin == null) {
            plugin = new ModelPlugin();
        }
        return plugin;
    }

    static BundleContext getContext() {
        return context;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        ModelPlugin.context = bundleContext;

        cleanupStateDir();
    }

    private void cleanupStateDir() {
        final String ndir = ErlangEngine.getInstance().getStateDir();
        final File fdir = new File(ndir);
        for (final File f : fdir.listFiles()) {
            if (f.isFile()) {
                f.delete();
            }
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        ModelPlugin.context = null;
        super.stop(bundleContext);
    }

    private IRpcSiteProvider getRuntimeProvider() {
        return ExtensionUtils.getSingletonExtension(
                "org.erlide.backend.backend", IRpcSiteProvider.class);
    }

    public IRpcSite getIdeBackend() {
        final IRpcSiteProvider provider = getRuntimeProvider();
        return provider.get();
    }

    public IRpcSite getBackend(final RuntimeVersion version) {
        final IRpcSiteProvider provider = getRuntimeProvider();
        return provider.get(version);
    }

    public IRpcSite getBackend(final IProject project) {
        final IRpcSiteProvider provider = getRuntimeProvider();
        return provider.get(project.getName());
    }

}
