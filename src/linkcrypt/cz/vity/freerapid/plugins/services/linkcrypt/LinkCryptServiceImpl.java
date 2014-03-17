package cz.vity.freerapid.plugins.services.linkcrypt;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class LinkCryptServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "linkcrypt.ws";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkCryptFileRunner();
    }

}