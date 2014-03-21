package cz.vity.freerapid.plugins.services.filespace;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileSpaceServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileSpace";
    }

    @Override
    public String getName() {
        return "filespace.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileSpaceFileRunner();
    }

}