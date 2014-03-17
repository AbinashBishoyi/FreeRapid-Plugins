package cz.vity.freerapid.plugins.services.filevelocity;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileVelocityServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileVelocity";
    }

    @Override
    public String getName() {
        return "filevelocity.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileVelocityFileRunner();
    }

}