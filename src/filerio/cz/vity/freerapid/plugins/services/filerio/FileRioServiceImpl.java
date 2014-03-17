package cz.vity.freerapid.plugins.services.filerio;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileRioServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "FileRio";
    }

    @Override
    public String getName() {
        return "filerio.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileRioFileRunner();
    }
}