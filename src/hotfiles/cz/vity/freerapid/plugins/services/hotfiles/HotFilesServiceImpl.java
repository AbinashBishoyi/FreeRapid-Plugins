package cz.vity.freerapid.plugins.services.hotfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class HotFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HotFiles";
    }

    @Override
    public String getName() {
        return "hotfiles.ws";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HotFilesFileRunner();
    }

}