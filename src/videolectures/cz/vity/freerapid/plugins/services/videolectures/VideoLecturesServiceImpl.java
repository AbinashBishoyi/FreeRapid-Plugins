package cz.vity.freerapid.plugins.services.videolectures;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class VideoLecturesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "videolectures.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VideoLecturesFileRunner();
    }

}