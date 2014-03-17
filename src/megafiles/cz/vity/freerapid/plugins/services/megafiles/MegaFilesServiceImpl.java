package cz.vity.freerapid.plugins.services.megafiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MegaFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MegaFiles";
    }

    @Override
    public String getName() {
        return "megafiles.se";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaFilesFileRunner();
    }

}