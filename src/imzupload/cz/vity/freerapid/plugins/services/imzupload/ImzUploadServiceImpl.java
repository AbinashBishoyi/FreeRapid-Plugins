package cz.vity.freerapid.plugins.services.imzupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class ImzUploadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "imzupload.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //don't forget to update this value, in plugin.xml don't forget to update this value too
        return 3;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImzUploadFileRunner();
    }

}