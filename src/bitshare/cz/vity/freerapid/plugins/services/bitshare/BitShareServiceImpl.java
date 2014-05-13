package cz.vity.freerapid.plugins.services.bitshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Stan
 */
public class BitShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "bitshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BitShareFileRunner();
    }

}