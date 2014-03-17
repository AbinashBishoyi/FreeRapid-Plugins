package cz.vity.freerapid.plugins.services.unibytes;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author RickCL
 */
public class UnibytesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "unibytes.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UnibytesFileRunner();
    }

}