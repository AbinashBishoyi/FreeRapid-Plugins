package cz.vity.freerapid.plugins.services.rodfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RodFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RodFile";
    }

    @Override
    public String getName() {
        return "rodfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RodFileFileRunner();
    }

}