package cz.vity.freerapid.plugins.services.hulkload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HulkLoadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HulkLoad";
    }

    @Override
    public String getName() {
        return "hulkload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HulkLoadFileRunner();
    }

}