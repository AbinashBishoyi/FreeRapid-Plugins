package cz.vity.freerapid.plugins.services.megasharevn;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class MegaShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "megashare.vn";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaShareFileRunner();
    }

}