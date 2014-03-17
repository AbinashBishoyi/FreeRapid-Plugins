package cz.vity.freerapid.plugins.services.asixfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class AsixFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AsixFiles";
    }

    @Override
    public String getName() {
        return "asixfiles.com";
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new AsixFilesFileRunner();
    }

}