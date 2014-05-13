package cz.vity.freerapid.plugins.services.sh;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author CrazyCoder
 */
public class ShServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "sh.st";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShFileRunner();
    }

}
