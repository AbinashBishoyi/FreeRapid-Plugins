package cz.vity.freerapid.plugins.services.pinterest;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author bircie
 */
public class PinterestServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pinterest.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PinterestFileRunner();
    }

}