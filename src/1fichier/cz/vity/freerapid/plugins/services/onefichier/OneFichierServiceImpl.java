package cz.vity.freerapid.plugins.services.onefichier;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OneFichierServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "1fichier.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OneFichierFileRunner();
    }

}