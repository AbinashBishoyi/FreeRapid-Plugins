package cz.vity.freerapid.plugins.services.ishared;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ISharedServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ishared.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ISharedFileRunner();
    }

}