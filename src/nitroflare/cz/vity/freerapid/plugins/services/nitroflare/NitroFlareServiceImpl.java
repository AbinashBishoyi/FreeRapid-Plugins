package cz.vity.freerapid.plugins.services.nitroflare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class NitroFlareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "nitroflare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NitroFlareFileRunner();
    }

}