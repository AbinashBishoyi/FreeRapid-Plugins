package cz.vity.freerapid.plugins.services.protectmylinks;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class ProtectMyLinksServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "protect-my-links.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ProtectMyLinksFileRunner();
    }

}