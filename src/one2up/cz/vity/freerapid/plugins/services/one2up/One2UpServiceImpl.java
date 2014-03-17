package cz.vity.freerapid.plugins.services.one2up;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class One2UpServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "one2up.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new One2UpFileRunner();
    }

}