package cz.vity.freerapid.plugins.services.przeklej;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Eterad
 */
public class PrzeklejServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "przeklej.pl";
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
        return new PrzeklejFileRunner();
    }

}
