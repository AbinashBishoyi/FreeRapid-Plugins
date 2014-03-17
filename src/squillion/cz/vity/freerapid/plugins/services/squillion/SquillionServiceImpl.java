package cz.vity.freerapid.plugins.services.squillion;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SquillionServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Squillion";
    }

    @Override
    public String getName() {
        return "squillion.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SquillionFileRunner();
    }

}