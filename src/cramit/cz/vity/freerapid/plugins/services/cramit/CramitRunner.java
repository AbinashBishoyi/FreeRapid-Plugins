package cz.vity.freerapid.plugins.services.cramit;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;

import java.util.List;

/**
 * @author RickCL
 */
public class CramitRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new CramitFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "to start your download");
        downloadPageMarkers.add(0, "your download will approximately take");
        downloadPageMarkers.add(0, "CLICK TO START DOWNLOAD");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<span class=green><b><a(?:.*?)href\\s?=\\s?(?:\"|')(.+?)(?:\"|')(?:.*?)>click here</a>");
        downloadLinkRegexes.add(0, "<a href=['\"]([^<>]+?)['\"][^<>]*?>\\s*CLICK TO START DOWNLOAD\\s*</a>");
        return downloadLinkRegexes;
    }

    @Override
    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = super.getCaptchaTypes();
        captchaTypes.add(0, new CramitCaptchaType());
        return captchaTypes;
    }
}
