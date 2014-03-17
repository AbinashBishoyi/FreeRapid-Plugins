package cz.vity.freerapid.plugins.services.fiberstorage;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FiberStorageServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FiberStorage";
    }

    @Override
    public String getName() {
        return "fiberstorage.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FiberStorageFileRunner();
    }

}