package cz.vity.freerapid.plugins.services.uploadcore;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadCoreFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("This Ticket Will be Active For Your Computer");
        return downloadPageMarkers;
    }

}