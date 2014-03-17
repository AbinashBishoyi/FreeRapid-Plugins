package cz.vity.freerapid.plugins.services.beatplexity;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author NicolasVega
 */
public class BeatplexityServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "beatplexity.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BeatplexityFileRunner();
    }

}