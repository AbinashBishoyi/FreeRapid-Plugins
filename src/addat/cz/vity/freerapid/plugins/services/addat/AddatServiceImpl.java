package cz.vity.freerapid.plugins.services.addat;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Javi
 */
public class AddatServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "addat.hu";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AddatFileRunner();
    }

}