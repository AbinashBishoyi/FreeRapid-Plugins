package cz.vity.freerapid.plugins.services.minus;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Tommy
 */
public class MinusServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getName() {
        return "minus.com";
    }

    @Override
    public String getServiceTitle() {
        return "Minus";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MinusFileRunner();
    }

}