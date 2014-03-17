package cz.vity.freerapid.plugins.services.uploads;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uploads.ws";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadsFileRunner();
    }

}