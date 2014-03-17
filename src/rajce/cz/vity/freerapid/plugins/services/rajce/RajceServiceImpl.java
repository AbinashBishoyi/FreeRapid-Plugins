package cz.vity.freerapid.plugins.services.rajce;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class RajceServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rajce.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RajceFileRunner();
    }

}