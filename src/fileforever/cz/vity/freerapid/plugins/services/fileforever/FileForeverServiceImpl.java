package cz.vity.freerapid.plugins.services.fileforever;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileForeverServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileForever";
    }

    @Override
    public String getName() {
        return "fileforever.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileForeverFileRunner();
    }

}