package cz.vity.freerapid.plugins.services.blitzfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BlitzFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BlitzFiles";
    }

    @Override
    public String getName() {
        return "blitzfiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BlitzFilesFileRunner();
    }

}