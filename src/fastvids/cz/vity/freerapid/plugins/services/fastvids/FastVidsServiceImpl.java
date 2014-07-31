package cz.vity.freerapid.plugins.services.fastvids;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FastVidsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Fast-Vids";
    }

    @Override
    public String getName() {
        return "fast-vids.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FastVidsFileRunner();
    }

}