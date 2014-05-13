package cz.vity.freerapid.plugins.services.filecore;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileCoreServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileCore";
    }

    @Override
    public String getName() {
        return "filecore.co.nz";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileCoreFileRunner();
    }

}