package cz.vity.freerapid.plugins.services.medafire;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MedaFireServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MedaFire";
    }

    @Override
    public String getName() {
        return "medafire.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MedaFireFileRunner();
    }

}