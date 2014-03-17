package cz.vity.freerapid.plugins.services.fileking;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileKingServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileKing";
    }

    @Override
    public String getName() {
        return "fileking.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileKingFileRunner();
    }

}