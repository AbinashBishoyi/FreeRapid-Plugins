package cz.vity.freerapid.plugins.services.yourfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 *         Class that provides basic info about plugin
 */
public class YourFilesServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "yourfiles.biz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YourFilesFileRunner();
    }

}
