package cz.vity.freerapid.plugins.services.filesear;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileSearServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileSear";
    }

    @Override
    public String getName() {
        return "filesear.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileSearFileRunner();
    }

}