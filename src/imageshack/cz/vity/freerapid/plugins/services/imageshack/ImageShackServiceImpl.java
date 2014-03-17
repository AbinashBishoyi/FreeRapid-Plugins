package cz.vity.freerapid.plugins.services.imageshack;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class ImageShackServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "imageshack.us";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImageShackFileRunner();
    }

}