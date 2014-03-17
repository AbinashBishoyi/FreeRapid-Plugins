package cz.vity.freerapid.plugins.services.idup;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class iDupServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "iDup";
    }

    @Override
    public String getName() {
        return "idup.in";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new iDupFileRunner();
    }

}