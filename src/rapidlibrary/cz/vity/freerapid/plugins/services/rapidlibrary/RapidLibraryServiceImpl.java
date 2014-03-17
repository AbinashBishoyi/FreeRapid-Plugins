package cz.vity.freerapid.plugins.services.rapidlibrary;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RapidLibraryServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "rapidlibrary.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidLibraryFileRunner();
    }

}