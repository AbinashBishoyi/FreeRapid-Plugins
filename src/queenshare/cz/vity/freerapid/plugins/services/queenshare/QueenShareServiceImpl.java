package cz.vity.freerapid.plugins.services.queenshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class QueenShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "QueenShare";
    }

    @Override
    public String getName() {
        return "queenshare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new QueenShareFileRunner();
    }

}