package cz.vity.freerapid.plugins.services.pornoxo;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PornOXOServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pornoxo.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PornOXOFileRunner();
    }

}