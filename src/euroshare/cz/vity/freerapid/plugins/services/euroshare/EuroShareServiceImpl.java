package cz.vity.freerapid.plugins.services.euroshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class EuroShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "euroshare.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EuroShareFileRunner();
    }

}