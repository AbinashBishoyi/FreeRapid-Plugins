package cz.vity.freerapid.plugins.services.pbs;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class PbsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pbs.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PbsFileRunner();
    }

}