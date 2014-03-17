package cz.vity.freerapid.plugins.services.filebitnet;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class FilebitNetFilesShareServiceImpl extends XFileSharingServiceImpl {
    private static final String SERVICE_NAME = "file-bit.net";

    @Override
    public String getServiceTitle() {
        return "File-Bit";
    }

    public String getName() {
        return "file-bit.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilebitNetFilesRunner();
    }
}