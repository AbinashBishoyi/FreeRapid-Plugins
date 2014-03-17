package cz.vity.freerapid.plugins.services.filebox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kajda
 * @since 0.82
 */
class FileboxFileRunner extends XFileSharingRunner {
    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Access from your region is not allowed")) {
            throw new PluginImplementationException("Access from your region is not allowed");
        }
        super.checkFileProblems();
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "product_download_url");
        downloadPageMarkers.add(0, ">> Download File <<");
        downloadPageMarkers.add(0, ">Download File<");
        downloadPageMarkers.add(0, "flowplayer(");
        downloadPageMarkers.add(0, "\" value='Download'");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("product_download_url=[\"']?(.+?)[\"']?>");
        downloadLinkRegexes.add("href=\"(.+?)\">>>> Download File");
        downloadLinkRegexes.add(0, "<input [^<>]+?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("product_file_name=") && getContentAsString().contains("&product_download_url")) {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "product_file_name=", "&product_download_url").trim());
        } else if (getContentAsString().contains("File Name : <span>")) {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "File Name : <span>", "</>").trim());
        } else if (getContentAsString().contains("File Name : </strong>")) {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "File Name : </strong>", "<br").trim());
        } else if (getContentAsString().contains("<a href=\"" + fileURL + "\">")) {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<a href=\"" + fileURL + "\">", "</a>").trim());
        } else throw new PluginImplementationException("File name not found");
        return super.getDownloadLinkFromRegexes();
    }

    @Override
    protected int getWaitTime() throws Exception {
        int retWaitTime = super.getWaitTime();
        if (retWaitTime == 0) {
            final Matcher matcher = getMatcherAgainstContent("id=\'countdown_str\'.*?<span id=\".*?\">.*?(\\d+).*?</span");
            if (matcher.find()) {
                retWaitTime = Integer.parseInt(matcher.group(1)) + 1;
            }
        }
        return retWaitTime;
    }
}