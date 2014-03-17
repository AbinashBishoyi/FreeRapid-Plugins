package cz.vity.freerapid.plugins.services.yleareena;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class YleAreenaServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "areena.yle.fi";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YleAreenaFileRunner();
    }

}