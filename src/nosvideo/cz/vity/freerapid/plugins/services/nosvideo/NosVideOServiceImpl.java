package cz.vity.freerapid.plugins.services.nosvideo;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class NosVideOServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "NosVideO";
    }

    @Override
    public String getName() {
        return "nosvideo.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NosVideOFileRunner();
    }

}