package cz.vity.freerapid.plugins.services.expressleech;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ExpressLeechServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ExpressLeech";
    }

    @Override
    public String getName() {
        return "expressleech.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ExpressLeechFileRunner();
    }

}