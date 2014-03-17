package cz.vity.freerapid.plugins.services.unlimitzone;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UnlimitZoneServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UnlimitZone";
    }

    @Override
    public String getName() {
        return "unlimitzone.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UnlimitZoneFileRunner();
    }

}