package cz.vity.freerapid.plugins.services.slingfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author TommyTom, ntoskrnl
 */
public class SlingFileServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "slingfile.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SlingFileFileRunner();
    }

}