package cz.vity.freerapid.plugins.services.filegag;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileGagServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileGag";
    }

    @Override
    public String getName() {
        return "filegag.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileGagFileRunner();
    }

}