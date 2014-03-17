package cz.vity.freerapid.plugins.services.fourshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FourShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "up.4share.vn";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FourShareFileRunner();
    }

}