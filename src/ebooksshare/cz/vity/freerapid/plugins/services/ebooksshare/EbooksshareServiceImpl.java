package cz.vity.freerapid.plugins.services.ebooksshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Abinash Bishoyi
 */
public class EbooksshareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ebooks-share.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EbooksshareFileRunner();
    }

}