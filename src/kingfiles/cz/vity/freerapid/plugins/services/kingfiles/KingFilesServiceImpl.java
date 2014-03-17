package cz.vity.freerapid.plugins.services.kingfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class KingFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "KingFiles";
    }

    @Override
    public String getName() {
        return "kingfiles.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KingFilesFileRunner();
    }

}