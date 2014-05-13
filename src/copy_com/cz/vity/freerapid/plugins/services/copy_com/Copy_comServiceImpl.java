package cz.vity.freerapid.plugins.services.copy_com;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Copy_comServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "copy.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Copy_comFileRunner();
    }

}