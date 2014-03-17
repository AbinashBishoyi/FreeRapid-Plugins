package cz.vity.freerapid.plugins.services.fdnlinks;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Vity
 */
public class FdnLinksServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "fdnlinks.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //don't forget to update this value, in plugin.xml don't forget to update this value too
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FdnLinksFileRunner();
    }

}