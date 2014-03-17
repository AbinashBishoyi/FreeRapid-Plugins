package cz.vity.freerapid.plugins.services.megaload_it;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class MegaLoad_itFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Your Download Link Has Been Generated");
        return downloadPageMarkers;
    }

}