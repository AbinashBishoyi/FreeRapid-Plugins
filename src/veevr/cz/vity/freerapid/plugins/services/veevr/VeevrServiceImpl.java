package cz.vity.freerapid.plugins.services.veevr;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class VeevrServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "veevr.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VeevrFileRunner();
    }

}