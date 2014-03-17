package cz.vity.freerapid.plugins.services.solidfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SolidFilesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "solidfiles.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SolidFilesFileRunner();
    }

}