package cz.vity.freerapid.plugins.services.sendspacepl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Eterad
 */
public class SendSpacePlServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "sendspace.pl";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SendSpacePlFileRunner();
    }

}
