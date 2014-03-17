package cz.vity.freerapid.plugins.services.appletrailers;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class AppleTrailersServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "trailers.apple.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AppleTrailersFileRunner();
    }

}