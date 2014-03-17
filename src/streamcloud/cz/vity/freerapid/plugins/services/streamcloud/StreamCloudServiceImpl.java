package cz.vity.freerapid.plugins.services.streamcloud;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class StreamCloudServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "StreamCloud";
    }

    @Override
    public String getName() {
        return "streamcloud.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StreamCloudFileRunner();
    }

}