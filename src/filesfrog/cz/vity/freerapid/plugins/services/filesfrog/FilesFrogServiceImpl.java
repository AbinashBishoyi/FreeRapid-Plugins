package cz.vity.freerapid.plugins.services.filesfrog;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FilesFrogServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FilesFrog";
    }

    @Override
    public String getName() {
        return "filesfrog.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilesFrogFileRunner();
    }

}