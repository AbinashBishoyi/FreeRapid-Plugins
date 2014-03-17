package cz.vity.freerapid.plugins.services.gigapeta;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Thumb
 */
public class GigaPetaServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "gigapeta.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GigaPetaFileRunner();
    }

}
