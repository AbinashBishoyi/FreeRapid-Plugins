package cz.vity.freerapid.plugins.services.u115;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Meow
 */
public class u115ServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "u115.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 20;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new u115FileRunner();
    }

}