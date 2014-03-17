package cz.vity.freerapid.plugins.services.adrive;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ADriveServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "adrive.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ADriveFileRunner();
    }

}