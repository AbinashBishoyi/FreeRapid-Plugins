package cz.vity.freerapid.plugins.services.bayfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class BayFilesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bayfiles.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BayFilesFileRunner();
    }

}