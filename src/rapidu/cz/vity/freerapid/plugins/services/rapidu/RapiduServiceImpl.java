package cz.vity.freerapid.plugins.services.rapidu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RapiduServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rapidu.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapiduFileRunner();
    }

}