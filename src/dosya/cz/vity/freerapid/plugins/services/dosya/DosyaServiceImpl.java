package cz.vity.freerapid.plugins.services.dosya;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DosyaServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Dosya";
    }

    @Override
    public String getName() {
        return "dosya.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DosyaFileRunner();
    }

}