package cz.vity.freerapid.plugins.services.datoid;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DatoidServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "datoid.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DatoidFileRunner();
    }

}