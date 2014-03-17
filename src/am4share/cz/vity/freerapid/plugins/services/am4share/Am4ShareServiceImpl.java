package cz.vity.freerapid.plugins.services.am4share;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Am4ShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Am4Share";
    }

    @Override
    public String getName() {
        return "am4share.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Am4ShareFileRunner();
    }

}