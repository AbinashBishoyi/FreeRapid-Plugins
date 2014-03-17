package cz.vity.freerapid.plugins.services.wat;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class WatServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "wat.tv";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WatFileRunner();
    }

}