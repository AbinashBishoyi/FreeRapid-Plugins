package cz.vity.freerapid.plugins.services.fbit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class FBitServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "f-bit.ru";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FBitFileRunner();
    }

}