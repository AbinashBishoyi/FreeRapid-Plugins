package cz.vity.freerapid.plugins.services.extrashare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Javi
 */
public class eXtraShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "extrashare.us";
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new eXtraShareFileRunner();
    }

}