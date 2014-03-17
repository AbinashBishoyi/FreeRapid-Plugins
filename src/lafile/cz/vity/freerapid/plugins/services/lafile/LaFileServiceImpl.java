package cz.vity.freerapid.plugins.services.lafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "LaFile";
    }

    @Override
    public String getName() {
        return "lafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LaFileFileRunner();
    }

}