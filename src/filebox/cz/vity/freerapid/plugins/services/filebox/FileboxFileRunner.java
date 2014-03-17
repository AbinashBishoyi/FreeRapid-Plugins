package cz.vity.freerapid.plugins.services.filebox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.LinkedList;
import java.util.List;

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
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("product_download_url=[\"']?(.+?)[\"']?>");
        downloadLinkRegexes.add("href=\"(.+?)\">>>> Download File");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        try {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "product_file_name=", "&product_download_url"));
        } catch (Exception e) {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "File Name : <span>", "</span>"));
        }
        return super.getDownloadLinkFromRegexes();
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        //they don't close <form> tag :p
        return getMethodBuilder()
                .setReferer(fileURL).setAction(fileURL)
                .setParameter("op", PlugUtils.getStringBetween(getContentAsString(), "name=\"op\" value=\"", "\">"))
                .setParameter("id", PlugUtils.getStringBetween(getContentAsString(), "name=\"id\" value=\"", "\">"))
                .setParameter("rand", PlugUtils.getStringBetween(getContentAsString(), "name=\"rand\" value=\"", "\">"))
                .setParameter("method_free", "1")
                .setParameter("down_direct", PlugUtils.getStringBetween(getContentAsString(), "name=\"down_direct\" value=\"", "\">"));
    }
}