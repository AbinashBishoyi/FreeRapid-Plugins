package cz.vity.freerapid.plugins.services.vidxden;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class VidXDenServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidXDen";
    }

    @Override
    public String getName() {
        return "vidxden.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidXDenFileRunner();
    }

}