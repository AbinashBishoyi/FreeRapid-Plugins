package cz.vity.freerapid.plugins.services.fourupfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FourUpFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "4UpFiles";
    }

    @Override
    public String getName() {
        return "4upfiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FourUpFilesFileRunner();
    }

}