package cz.vity.freerapid.plugins.services.restfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RestFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RestFile";
    }

    @Override
    public String getName() {
        return "restfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RestFileFileRunner();
    }

}