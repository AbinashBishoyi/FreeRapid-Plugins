package cz.vity.freerapid.plugins.services.prefiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author CrazyCoder
 */
public class PreFilesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "prefiles.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PreFilesFileRunner();
    }

}