package cz.vity.freerapid.plugins.services.linksavegroup;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class LinksaveGroupServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "linksavegroup.in";
    }

    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinksaveGroupFileRunner();
    }

}
