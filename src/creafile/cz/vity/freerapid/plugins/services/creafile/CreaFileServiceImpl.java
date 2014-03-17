package cz.vity.freerapid.plugins.services.creafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CreaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CreaFile";
    }

    @Override
    public String getName() {
        return "creafile.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CreaFileFileRunner();
    }
}