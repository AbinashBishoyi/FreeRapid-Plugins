package cz.vity.freerapid.plugins.services.rutube;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RuTubeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rutube.ru";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RuTubeFileRunner();
    }

}