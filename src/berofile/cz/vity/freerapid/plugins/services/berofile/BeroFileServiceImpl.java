package cz.vity.freerapid.plugins.services.berofile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BeroFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BeroFile";
    }

    @Override
    public String getName() {
        return "berofile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BeroFileFileRunner();
    }

}