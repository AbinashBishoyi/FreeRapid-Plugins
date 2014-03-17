package cz.vity.freerapid.plugins.services.ardmediathek;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class ArdMediathekServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ardmediathek.de";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ArdMediathekFileRunner();
    }

}