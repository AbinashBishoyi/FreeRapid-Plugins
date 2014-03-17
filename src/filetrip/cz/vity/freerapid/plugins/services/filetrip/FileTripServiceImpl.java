package cz.vity.freerapid.plugins.services.filetrip;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileTripServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filetrip.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileTripFileRunner();
    }

}