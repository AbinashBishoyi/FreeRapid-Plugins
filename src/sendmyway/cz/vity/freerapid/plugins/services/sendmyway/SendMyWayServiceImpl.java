package cz.vity.freerapid.plugins.services.sendmyway;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class SendMyWayServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SendMyWay";
    }

    @Override
    public String getName() {
        return "sendmyway.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SendMyWayFileRunner();
    }

}