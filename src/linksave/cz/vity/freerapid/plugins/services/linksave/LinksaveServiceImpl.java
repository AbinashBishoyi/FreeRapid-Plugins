package cz.vity.freerapid.plugins.services.linksave;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Arthur Gunawan
 */
public class LinksaveServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "linksave.in";
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinksaveFileRunner();
    }

}
