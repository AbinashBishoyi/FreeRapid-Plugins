package cz.vity.freerapid.plugins.services.odsiebie;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Eterad
 */
public class OdSiebieServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "odsiebie.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OdSiebieFileRunner();
    }

}
