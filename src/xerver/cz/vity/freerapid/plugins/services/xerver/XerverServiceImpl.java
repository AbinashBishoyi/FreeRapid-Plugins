package cz.vity.freerapid.plugins.services.xerver;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class XerverServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Xerver";
    }

    @Override
    public String getName() {
        return "xerver.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new XerverFileRunner();
    }

}