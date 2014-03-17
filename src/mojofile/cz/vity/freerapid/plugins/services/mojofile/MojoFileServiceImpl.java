package cz.vity.freerapid.plugins.services.mojofile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MojoFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MojoFile";
    }

    @Override
    public String getName() {
        return "mojofile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MojoFileFileRunner();
    }

}