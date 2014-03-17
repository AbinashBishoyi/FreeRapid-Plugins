package cz.vity.freerapid.plugins.services.filesavr;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class FileSavrServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filesavr.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileSavrFileRunner();
    }

}