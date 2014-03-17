package cz.vity.freerapid.plugins.services.oron;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Thumb
 */
public class OronServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "oron.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OronFileRunner();
    }

}