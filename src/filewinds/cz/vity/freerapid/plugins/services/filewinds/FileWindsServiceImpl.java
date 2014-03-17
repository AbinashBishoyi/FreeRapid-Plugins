package cz.vity.freerapid.plugins.services.filewinds;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileWindsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileWinds";
    }

    @Override
    public String getName() {
        return "filewinds.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileWindsFileRunner();
    }

}