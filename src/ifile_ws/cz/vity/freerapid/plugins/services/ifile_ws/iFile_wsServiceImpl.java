package cz.vity.freerapid.plugins.services.ifile_ws;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class iFile_wsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "iFile";
    }

    @Override
    public String getName() {
        return "ifile.ws";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new iFile_wsFileRunner();
    }

}