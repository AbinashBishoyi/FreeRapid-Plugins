package cz.vity.freerapid.plugins.services.thefile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TheFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "TheFile";
    }

    @Override
    public String getName() {
        return "thefile.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TheFileFileRunner();
    }

}