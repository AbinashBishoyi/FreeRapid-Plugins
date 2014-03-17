package cz.vity.freerapid.plugins.services.venusfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VenusFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VenusFile";
    }

    @Override
    public String getName() {
        return "venusfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VenusFileFileRunner();
    }

}