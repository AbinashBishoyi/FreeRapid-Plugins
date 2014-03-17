package cz.vity.freerapid.plugins.services.brutalsha;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BrutalShaServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BrutalSha";
    }

    @Override
    public String getName() {
        return "brutalsha.re";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BrutalShaFileRunner();
    }

}