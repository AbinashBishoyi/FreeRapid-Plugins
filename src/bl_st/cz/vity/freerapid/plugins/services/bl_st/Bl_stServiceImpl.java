package cz.vity.freerapid.plugins.services.bl_st;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Bl_stServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Bl.st";
    }

    @Override
    public String getName() {
        return "bl.st";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Bl_stFileRunner();
    }

}