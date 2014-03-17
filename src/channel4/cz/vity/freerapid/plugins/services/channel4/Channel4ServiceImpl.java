package cz.vity.freerapid.plugins.services.channel4;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class Channel4ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "channel4.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Channel4FileRunner();
    }

}