package cz.vity.freerapid.plugins.services.lumfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LumFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "LumFile";
    }

    @Override
    public String getName() {
        return "lumfile.com";
    }


    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LumFileFileRunner();
    }

}