package cz.vity.freerapid.plugins.services.uploadjockey;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Alex, Arthur Gunawan
 */
public class UploadJockeyServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "uploadjockey.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadJockeyFileRunner();
    }

}