package cz.vity.freerapid.plugins.services.filesharero;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author TROJal.exe
 */
public class FileShareRoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "fileshare.ro";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileShareRoFileRunner();
    }

}