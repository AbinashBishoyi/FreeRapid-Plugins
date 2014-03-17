package cz.vity.freerapid.plugins.services.filenuke;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileNukeServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileNuke";
    }

    @Override
    public String getName() {
        return "filenuke.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileNukeFileRunner();
    }

}