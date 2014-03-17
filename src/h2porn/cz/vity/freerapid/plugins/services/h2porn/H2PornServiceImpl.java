package cz.vity.freerapid.plugins.services.h2porn;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class H2PornServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "h2porn.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new H2PornFileRunner();
    }

}