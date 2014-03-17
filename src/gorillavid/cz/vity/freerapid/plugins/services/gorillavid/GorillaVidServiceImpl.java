package cz.vity.freerapid.plugins.services.gorillavid;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class GorillaVidServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "GorillaVid";
    }

    @Override
    public String getName() {
        return "gorillavid.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GorillaVidFileRunner();
    }
}