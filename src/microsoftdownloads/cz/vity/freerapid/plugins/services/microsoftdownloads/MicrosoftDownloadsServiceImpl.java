package cz.vity.freerapid.plugins.services.microsoftdownloads;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class MicrosoftDownloadsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "microsoft.com/downloads";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MicrosoftDownloadsFileRunner();
    }

}