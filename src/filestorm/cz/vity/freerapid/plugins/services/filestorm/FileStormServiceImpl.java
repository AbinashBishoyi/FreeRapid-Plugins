package cz.vity.freerapid.plugins.services.filestorm;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileStormServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileStorm";
    }

    @Override
    public String getName() {
        return "filestorm.to";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileStormFileRunner();
    }

}