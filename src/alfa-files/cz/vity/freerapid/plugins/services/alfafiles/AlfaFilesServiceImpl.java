package cz.vity.freerapid.plugins.services.alfafiles;

import cz.vity.freerapid.plugins.services.alfafiles.AlfaFilesFileRunner;
import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author RickCL
 */
public class AlfaFilesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "alfa-files.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //don't forget to update this value, in plugin.xml don't forget to update this value too
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AlfaFilesFileRunner();
    }

}