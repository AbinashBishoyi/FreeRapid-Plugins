package cz.vity.freerapid.plugins.services.linkdecrypter;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class LinkDecrypterServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "linkdecrypter.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkDecrypterFileRunner();
    }

}