package cz.vity.freerapid.plugins.services.uaimage;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class UaImageServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uaimage.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UaImageFileRunner();
    }

}