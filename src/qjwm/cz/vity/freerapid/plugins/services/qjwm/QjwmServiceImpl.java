package cz.vity.freerapid.plugins.services.qjwm;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Tommy Yang
 */
public class QjwmServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getName() {
        return"qjwm.com";
    }

    @Override
    public String getServiceTitle() {
        return "Qjwm";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new QjwmRunner();
    }

}
