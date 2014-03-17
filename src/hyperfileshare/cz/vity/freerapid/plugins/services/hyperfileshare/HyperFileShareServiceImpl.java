package cz.vity.freerapid.plugins.services.hyperfileshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class HyperFileShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "hyperfileshare.com";
    }


    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HyperFileShareFileRunner();
    }

}