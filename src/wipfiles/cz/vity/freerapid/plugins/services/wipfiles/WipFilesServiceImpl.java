package cz.vity.freerapid.plugins.services.wipfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class WipFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "WipFiles";
    }

    @Override
    public String getName() {
        return "wipfiles.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WipFilesFileRunner();
    }
}