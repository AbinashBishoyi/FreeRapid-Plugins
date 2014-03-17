package cz.vity.freerapid.plugins.services.hipfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HipFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HipFile";
    }

    @Override
    public String getName() {
        return "hipfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HipFileFileRunner();
    }

}