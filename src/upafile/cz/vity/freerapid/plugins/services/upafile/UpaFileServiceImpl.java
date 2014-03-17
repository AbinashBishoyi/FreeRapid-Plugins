package cz.vity.freerapid.plugins.services.upafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UpaFile";
    }

    @Override
    public String getName() {
        return "upafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpaFileFileRunner();
    }

}