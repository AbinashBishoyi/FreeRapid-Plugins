package cz.vity.freerapid.plugins.services.rtve;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RtveServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rtve.es";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RtveFileRunner();
    }

}