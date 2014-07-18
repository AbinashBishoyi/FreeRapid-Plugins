package cz.vity.freerapid.plugins.services.vshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VShare";
    }

    @Override
    public String getName() {
        return "vshare.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VShareFileRunner();
    }

}