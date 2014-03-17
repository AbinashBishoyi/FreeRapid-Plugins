package cz.vity.freerapid.plugins.services.deerfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DeerFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DeerFile";
    }

    @Override
    public String getName() {
        return "deerfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DeerFileFileRunner();
    }

}