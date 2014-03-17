package cz.vity.freerapid.plugins.services.megauri;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class MegaUriServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "mega_uri";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaUriFileRunner();
    }

}