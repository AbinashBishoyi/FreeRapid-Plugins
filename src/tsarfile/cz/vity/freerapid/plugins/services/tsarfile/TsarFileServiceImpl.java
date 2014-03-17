package cz.vity.freerapid.plugins.services.tsarfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TsarFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "TsarFile";
    }

    @Override
    public String getName() {
        return "tsarfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TsarFileFileRunner();
    }

}