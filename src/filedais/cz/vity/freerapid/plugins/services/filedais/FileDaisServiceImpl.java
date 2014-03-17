package cz.vity.freerapid.plugins.services.filedais;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileDaisServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileDais";
    }

    @Override
    public String getName() {
        return "filedais.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileDaisFileRunner();
    }

}