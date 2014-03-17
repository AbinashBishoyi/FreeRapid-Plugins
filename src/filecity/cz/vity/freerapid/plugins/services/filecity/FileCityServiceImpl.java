package cz.vity.freerapid.plugins.services.filecity;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileCityServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filecity.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileCityFileRunner();
    }

}