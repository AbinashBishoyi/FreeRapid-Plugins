package cz.vity.freerapid.plugins.services.rghost;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RGhostServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rghost.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RGhostFileRunner();
    }

}