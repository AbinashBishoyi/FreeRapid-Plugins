package cz.vity.freerapid.plugins.services.barrandov;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class BarrandovServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "barrandov.tv";
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
        return new BarrandovFileRunner();
    }

}