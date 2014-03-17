package cz.vity.freerapid.plugins.services.vacishare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VaciShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VaciShare";
    }

    @Override
    public String getName() {
        return "vacishare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VaciShareFileRunner();
    }

}