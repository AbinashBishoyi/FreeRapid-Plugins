package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class JalurCepatServiceImpl extends AbstractFileShareService {
    @Override
    public String getName() {
        return "jalurcepat.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new JalurCepatFileRunner();
    }

    
}