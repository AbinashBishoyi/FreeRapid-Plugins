package cz.vity.freerapid.plugins.services.ilix;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class IlixServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "ilix.in";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IlixFileRunner();
    }

}