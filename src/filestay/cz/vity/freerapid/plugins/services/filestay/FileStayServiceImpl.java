package cz.vity.freerapid.plugins.services.filestay;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie\
 */
public class FileStayServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileStay";
    }

    @Override
    public String getName() {
        return "filestay.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileStayFileRunner();
    }

}