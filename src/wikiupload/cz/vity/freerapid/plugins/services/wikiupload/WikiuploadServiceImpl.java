package cz.vity.freerapid.plugins.services.wikiupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class WikiuploadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "wikiupload.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WikiuploadFileRunner();
    }

}
