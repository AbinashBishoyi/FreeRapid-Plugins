package cz.vity.freerapid.plugins.services.spankwire;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author User
 */
public class SpankWireServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "spankwire.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpankWireFileRunner();
    }


}