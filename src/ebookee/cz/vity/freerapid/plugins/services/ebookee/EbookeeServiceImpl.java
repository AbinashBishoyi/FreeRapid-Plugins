package cz.vity.freerapid.plugins.services.ebookee;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Abinash Bishoyi
 */
public class EbookeeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ebookee.ws";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EbookeeFileRunner();
    }

}