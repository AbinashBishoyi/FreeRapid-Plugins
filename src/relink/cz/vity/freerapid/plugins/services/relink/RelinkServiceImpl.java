package cz.vity.freerapid.plugins.services.relink;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author benpicco
 */
public class RelinkServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "relink.us";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//Check not supported
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RelinkFileRunner();
    }

}
