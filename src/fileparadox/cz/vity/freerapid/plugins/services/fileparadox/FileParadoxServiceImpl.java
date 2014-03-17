package cz.vity.freerapid.plugins.services.fileparadox;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileParadoxServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileParadox";
    }

    @Override
    public String getName() {
        return "fileparadox.in";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileParadoxFileRunner();
    }

}