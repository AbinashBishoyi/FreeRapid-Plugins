package cz.vity.freerapid.plugins.services.billionuploads;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

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
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?)(?:\"|') id=\"_tlink\"");
        return downloadLinkRegexes;
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