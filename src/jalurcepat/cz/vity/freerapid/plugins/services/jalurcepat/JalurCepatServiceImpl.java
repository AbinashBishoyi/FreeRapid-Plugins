package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class JalurCepatServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "JalurCepat";
    }

    @Override
    public String getName() {
        return "jalurcepat.com";
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new JalurCepatFileRunner();
    }

}