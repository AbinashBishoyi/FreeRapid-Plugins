package cz.vity.freerapid.plugins.services.billionuploads;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BillionUploadsFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new BillionUploadsFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new BillionUploadsFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Download you file easily with Billion Uploads download manager");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?)(?:\"|') id=\"_tlink\"");
        downloadLinkRegexes.add("<span subway=\"metro\">.+?XXX(.+?)XXX.+?<");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        final String url = super.getDownloadLinkFromRegexes();
        if (url.contains("http:"))
            return url;
        return new String(Base64.decodeBase64(new String(Base64.decodeBase64(url))));
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final String secretInput = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "decodeURIComponent(\"", "\")"), "UTF-8");
        final String name = PlugUtils.getStringBetween(secretInput, " name=\"", "\"");
        final String value = PlugUtils.getStringBetween(secretInput, " value=\"", "\"");
        return super.getXFSMethodBuilder().setParameter(name, value);
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not found") || content.contains("File was removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void setLanguageCookie() throws Exception {
        setSecurityPageCookie();
        super.setLanguageCookie();
    }

    private void setSecurityPageCookie() throws Exception {
        final HttpMethod method = getGetMethod(fileURL);
        makeRequest(method);
        if (getContentAsString().contains("This process is automatic. Your browser will redirect to your requested content shortly")) {
            Integer eval = evaluate(PlugUtils.getStringBetween(getContentAsString(), "a.value =", ";"));
            final String answer = "" + (eval + ("billionuploads.com").length());
            final HttpMethod securityMethod = getMethodBuilder()
                    .setActionFromFormByName("challenge-form", true)
                    .setParameter("jschl_answer", answer)
                    .toGetMethod();

            makeRequest(securityMethod);
            super.checkFileProblems();
        }

    }


    private int evaluate(final String equationString) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("(\\d+)(\\D+)?(\\d+)?(\\D+)?(\\d+)?(\\D+)?(\\d+)?(\\D+)?", equationString);
        if (!match.find()) throw new ErrorDuringDownloadingException();
        List<String> equation = new LinkedList<String>();
        for (int ii = 1; ii <= match.groupCount(); ii++) {
            if (match.group(ii) != null)
                equation.add(match.group(ii));
        }
        for (int ii = 0; ii < equation.size(); ii++) {
            if (equation.get(ii).equals("*")) {
                update(equation, ii, (Integer.parseInt(equation.get(ii - 1)) * Integer.parseInt(equation.get(ii + 1))));
                ii = 0;
            } else if (equation.get(ii).equals("/")) {
                update(equation, ii, (Integer.parseInt(equation.get(ii - 1)) / Integer.parseInt(equation.get(ii + 1))));
                ii = 0;
            }
        }
        for (int ii = 0; ii < equation.size(); ii++) {
            if (equation.get(ii).equals("+")) {
                update(equation, ii, (Integer.parseInt(equation.get(ii - 1)) + Integer.parseInt(equation.get(ii + 1))));
                ii = 0;
            } else if (equation.get(ii).equals("-")) {
                update(equation, ii, (Integer.parseInt(equation.get(ii - 1)) - Integer.parseInt(equation.get(ii + 1))));
                ii = 0;
            }
        }
        return Integer.parseInt(equation.get(0));
    }

    private void update(List<String> equation, int ii, int newValue) {
        equation.set(ii - 1, "" + newValue);
        equation.remove(ii + 1);
        equation.remove(ii);
    }
}