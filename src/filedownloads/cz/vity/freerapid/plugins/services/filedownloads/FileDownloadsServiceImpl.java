package cz.vity.freerapid.plugins.services.filedownloads;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileDownloadsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileDownloads";
    }

    @Override
    public String getName() {
        return "filedownloads.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileDownloadsFileRunner();
    }

}