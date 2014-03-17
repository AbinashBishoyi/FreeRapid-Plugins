package cz.vity.freerapid.plugins.services.adf;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class AdfServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "adf.ly";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AdfFileRunner();
    }

}