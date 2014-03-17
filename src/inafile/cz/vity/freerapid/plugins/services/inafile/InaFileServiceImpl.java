package cz.vity.freerapid.plugins.services.inafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class InaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "InaFile";
    }

    @Override
    public String getName() {
        return "inafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new InaFileFileRunner();
    }
}