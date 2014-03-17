package cz.vity.freerapid.plugins.services.mxua;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MxuaServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Mxua";
    }

    @Override
    public String getName() {
        return "mxua.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MxuaFileRunner();
    }

}