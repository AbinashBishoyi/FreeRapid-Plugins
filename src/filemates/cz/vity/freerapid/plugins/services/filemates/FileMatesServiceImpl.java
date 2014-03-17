package cz.vity.freerapid.plugins.services.filemates;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileMatesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileMates";
    }

    @Override
    public String getName() {
        return "filemates.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileMatesFileRunner();
    }

}