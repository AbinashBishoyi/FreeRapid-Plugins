package cz.vity.freerapid.plugins.services.noovashare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class NoovaShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "NoovaShare";
    }

    @Override
    public String getName() {
        return "noovashare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NoovaShareFileRunner();
    }

}