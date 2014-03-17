package cz.vity.freerapid.plugins.services.hugefiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HugeFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HugeFiles";
    }

    @Override
    public String getName() {
        return "hugefiles.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HugeFilesFileRunner();
    }

}