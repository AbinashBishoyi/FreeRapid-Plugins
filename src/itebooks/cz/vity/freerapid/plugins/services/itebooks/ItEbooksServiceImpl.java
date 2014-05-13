package cz.vity.freerapid.plugins.services.itebooks;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class ItEbooksServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "it-ebooks.info";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ItEbooksFileRunner();
    }

}