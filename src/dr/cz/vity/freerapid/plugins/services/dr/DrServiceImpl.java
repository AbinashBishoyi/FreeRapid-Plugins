package cz.vity.freerapid.plugins.services.dr;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class DrServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "dr.dk";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DrFileRunner();
    }

}