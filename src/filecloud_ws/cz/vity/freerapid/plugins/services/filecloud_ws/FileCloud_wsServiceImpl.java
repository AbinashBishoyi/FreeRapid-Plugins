package cz.vity.freerapid.plugins.services.filecloud_ws;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileCloud_wsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileCloud_ws";
    }

    @Override
    public String getName() {
        return "filecloud.ws";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileCloud_wsFileRunner();
    }

}