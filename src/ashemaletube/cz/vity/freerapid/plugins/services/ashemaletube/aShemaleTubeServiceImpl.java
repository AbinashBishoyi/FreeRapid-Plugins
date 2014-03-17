package cz.vity.freerapid.plugins.services.ashemaletube;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class aShemaleTubeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ashemaletube.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new aShemaleTubeFileRunner();
    }

}