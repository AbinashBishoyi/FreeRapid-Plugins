package cz.vity.freerapid.plugins.services.gigaup;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class GigaUPServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "gigaup.fr";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GigaUPFileRunner();
    }

}