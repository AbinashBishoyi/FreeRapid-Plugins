package cz.vity.freerapid.plugins.services.uploadrive;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploaDriveServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploaDrive";
    }

    @Override
    public String getName() {
        return "uploadrive.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploaDriveFileRunner();
    }

}