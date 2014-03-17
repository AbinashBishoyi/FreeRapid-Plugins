package cz.vity.freerapid.plugins.services.mafiastorage;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MafiaStorageServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MafiaStorage";
    }

    @Override
    public String getName() {
        return "mafiastorage.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MafiaStorageFileRunner();
    }

}