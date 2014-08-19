package cz.vity.freerapid.plugins.services.daclips;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DaClipsFileRunner extends XFilePlayerRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("daclips\\.com", "daclips.in");
    }
}