package cz.vity.freerapid.plugins.services.fourstreaming;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FourStreamingServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "4Streaming";
    }

    @Override
    public String getName() {
        return "4streaming.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FourStreamingFileRunner();
    }

}