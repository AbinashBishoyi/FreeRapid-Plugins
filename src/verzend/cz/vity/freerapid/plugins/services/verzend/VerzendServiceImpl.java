package cz.vity.freerapid.plugins.services.verzend;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VerzendServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Verzend";
    }

    @Override
    public String getName() {
        return "verzend.be";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VerzendFileRunner();
    }

}