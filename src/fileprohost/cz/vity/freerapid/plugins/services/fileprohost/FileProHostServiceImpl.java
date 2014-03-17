package cz.vity.freerapid.plugins.services.fileprohost;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileProHostServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileProHost";
    }

    @Override
    public String getName() {
        return "fileprohost.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileProHostFileRunner();
    }

}