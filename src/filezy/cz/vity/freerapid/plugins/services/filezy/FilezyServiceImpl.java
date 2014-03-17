package cz.vity.freerapid.plugins.services.filezy;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FilezyServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Filezy";
    }

    @Override
    public String getName() {
        return "filezy.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilezyFileRunner();
    }

}