package cz.vity.freerapid.plugins.services.filedefend;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileDefendServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileDefend";
    }

    @Override
    public String getName() {
        return "filedefend.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileDefendFileRunner();
    }

}