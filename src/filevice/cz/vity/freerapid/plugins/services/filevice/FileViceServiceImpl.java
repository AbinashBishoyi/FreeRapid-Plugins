package cz.vity.freerapid.plugins.services.filevice;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileViceServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileVice";
    }

    @Override
    public String getName() {
        return "filevice.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileViceFileRunner();
    }

}