package cz.vity.freerapid.plugins.services.megaload_it;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MegaLoad_itServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MegaLoad_it";
    }

    @Override
    public String getName() {
        return "megaload.it";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaLoad_itFileRunner();
    }

}