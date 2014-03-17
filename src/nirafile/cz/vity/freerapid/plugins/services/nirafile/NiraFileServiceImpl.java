package cz.vity.freerapid.plugins.services.nirafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class NiraFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "NiraFile";
    }

    @Override
    public String getName() {
        return "nirafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NiraFileFileRunner();
    }

}