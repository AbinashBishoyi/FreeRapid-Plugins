package cz.vity.freerapid.plugins.services.filehostws;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileHostWsServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "FileHostWs";
    }

    @Override
    public String getName() {
        return "filehostws.ws";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileHostWsFileRunner();
    }
}