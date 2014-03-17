package cz.vity.freerapid.plugins.services.junocloud;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class JunoCloudServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "JunoCloud";
    }

    @Override
    public String getName() {
        return "junocloud.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new JunoCloudFileRunner();
    }

}