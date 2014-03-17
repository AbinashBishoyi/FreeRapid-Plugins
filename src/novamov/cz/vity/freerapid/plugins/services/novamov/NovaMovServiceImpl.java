package cz.vity.freerapid.plugins.services.novamov;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class NovaMovServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "NovaMov";
    }

    @Override
    public String getName() {
        return "novamov.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NovaMovFileRunner();
    }

}