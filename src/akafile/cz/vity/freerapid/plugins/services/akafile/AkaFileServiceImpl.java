package cz.vity.freerapid.plugins.services.akafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AkaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AkaFile";
    }

    @Override
    public String getName() {
        return "akafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AkaFileFileRunner();
    }

}