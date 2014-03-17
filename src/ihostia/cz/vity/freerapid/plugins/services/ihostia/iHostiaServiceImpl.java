package cz.vity.freerapid.plugins.services.ihostia;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class iHostiaServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "iHostia";
    }

    @Override
    public String getName() {
        return "ihostia.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new iHostiaFileRunner();
    }

}