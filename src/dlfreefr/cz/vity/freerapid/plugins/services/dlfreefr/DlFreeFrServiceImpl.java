package cz.vity.freerapid.plugins.services.dlfreefr;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class DlFreeFrServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "dl.free.fr";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DlFreeFrFileRunner();
    }

}