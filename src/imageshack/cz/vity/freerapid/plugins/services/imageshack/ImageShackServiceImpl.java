package cz.vity.freerapid.plugins.services.imageshack;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class ImageShackServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "imageshack.us";
    }

    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImageShackFileRunner();
    }

}