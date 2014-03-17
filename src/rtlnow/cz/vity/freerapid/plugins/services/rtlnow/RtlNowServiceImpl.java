package cz.vity.freerapid.plugins.services.rtlnow;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RtlNowServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rtl-now.rtl.de";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RtlNowFileRunner();
    }

}