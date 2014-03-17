package cz.vity.freerapid.plugins.services.getapp;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Eterad
 */
public class GetAppServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "getapp.info";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GetAppFileRunner();
    }

}
