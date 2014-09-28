package cz.vity.freerapid.plugins.services.filepom;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FilePomServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FilePom";
    }

    @Override
    public String getName() {
        return "filepom.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilePomFileRunner();
    }
}