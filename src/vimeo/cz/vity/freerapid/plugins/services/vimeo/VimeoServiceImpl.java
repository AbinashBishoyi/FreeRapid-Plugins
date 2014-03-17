package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class VimeoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "vimeo.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VimeoFileRunner();
    }

}