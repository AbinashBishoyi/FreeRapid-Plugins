package cz.vity.freerapid.plugins.services.vidto;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class VidToFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("var file_link = ");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a.+?href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("download1", true)                 //####
                .setAction(fileURL)
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if ((methodBuilder.getParameters().get("method_free") != null) && (!methodBuilder.getParameters().get("method_free").isEmpty())) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }
}