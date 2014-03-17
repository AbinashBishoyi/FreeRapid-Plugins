package cz.vity.freerapid.plugins.services.uppit;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * @author Kajda
 * @author ntoskrnl
 * @since 0.82
 */
class UppITFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Your download is starting");
        return downloadPageMarkers;
    }

}