package cz.vity.freerapid.plugins.services.usefile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UseFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UseFile";
    }

    @Override
    public String getName() {
        return "usefile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UseFileFileRunner();
    }

}