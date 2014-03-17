package cz.vity.freerapid.plugins.services.fireget;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FireGetServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FireGet";
    }

    @Override
    public String getName() {
        return "fireget.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FireGetFileRunner();
    }

}