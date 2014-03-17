package cz.vity.freerapid.plugins.services.uload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ULoadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ULoad";
    }

    @Override
    public String getName() {
        return "uload.to";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ULoadFileRunner();
    }

}