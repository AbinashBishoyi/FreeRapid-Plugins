package cz.vity.freerapid.plugins.services.channel5;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class Channel5ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "channel5.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Channel5FileRunner();
    }

}