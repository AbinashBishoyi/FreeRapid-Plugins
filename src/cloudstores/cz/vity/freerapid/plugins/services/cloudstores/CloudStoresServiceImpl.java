package cz.vity.freerapid.plugins.services.cloudstores;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class CloudStoresServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "cloudstor.es";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CloudStoresFileRunner();
    }

}