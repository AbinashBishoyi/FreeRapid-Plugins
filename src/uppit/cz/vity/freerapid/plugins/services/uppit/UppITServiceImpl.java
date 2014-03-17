package cz.vity.freerapid.plugins.services.uppit;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 * @author ntoskrnl
 */
public class UppITServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UppIt";
    }

    @Override
    public String getName() {
        return "uppit.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UppITFileRunner();
    }

}