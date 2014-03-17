package cz.vity.freerapid.plugins.services.ruutu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RuutuServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ruutu.fi";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RuutuFileRunner();
    }

}