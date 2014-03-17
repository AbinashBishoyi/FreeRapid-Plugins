package cz.vity.freerapid.plugins.services.ortofiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OrtoFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "OrtoFiles";
    }

    @Override
    public String getName() {
        return "ortofiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OrtoFilesFileRunner();
    }

}