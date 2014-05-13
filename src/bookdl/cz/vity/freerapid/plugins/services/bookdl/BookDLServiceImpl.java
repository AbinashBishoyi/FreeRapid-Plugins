package cz.vity.freerapid.plugins.services.bookdl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author CrazyCoder
 */
public class BookDLServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bookdl.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BookDLFileRunner();
    }

}