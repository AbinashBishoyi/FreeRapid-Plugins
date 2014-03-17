package cz.vity.freerapid.plugins.services.tusfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TusFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "TusFiles";
    }

    @Override
    public String getName() {
        return "tusfiles.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TusFilesFileRunner();
    }

}