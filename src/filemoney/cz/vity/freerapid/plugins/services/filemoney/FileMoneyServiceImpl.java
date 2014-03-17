package cz.vity.freerapid.plugins.services.filemoney;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileMoneyServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileMoney";
    }

    @Override
    public String getName() {
        return "filemoney.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileMoneyFileRunner();
    }

}