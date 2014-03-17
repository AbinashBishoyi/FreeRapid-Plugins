package cz.vity.freerapid.plugins.services.qkup;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class QkUpServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "QkUp";
    }

    @Override
    public String getName() {
        return "qkup.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new QkUpFileRunner();
    }

}