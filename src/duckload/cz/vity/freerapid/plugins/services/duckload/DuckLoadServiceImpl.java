package cz.vity.freerapid.plugins.services.duckload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class DuckLoadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "duckload.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 4;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DuckLoadFileRunner();
    }

}