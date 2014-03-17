package cz.vity.freerapid.plugins.services.filesabc;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FilesABCServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FilesABC";
    }

    @Override
    public String getName() {
        return "filesabc.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilesABCFileRunner();
    }

}