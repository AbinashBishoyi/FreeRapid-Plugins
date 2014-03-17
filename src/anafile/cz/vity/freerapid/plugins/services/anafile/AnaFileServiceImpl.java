package cz.vity.freerapid.plugins.services.anafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AnaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AnaFile";
    }

    @Override
    public String getName() {
        return "anafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AnaFileFileRunner();
    }

}