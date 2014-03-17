package cz.vity.freerapid.plugins.services.sendspace;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class SendspaceFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(SendspaceFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            checkSeriousProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            checkAllProblems();
            checkNameAndSize();
            /*
            final String contentAsString = getContentAsString();
            final String encodedLink = PlugUtils.getStringBetween(contentAsString, "base64ToText('", "')));");
            final int intParameter = PlugUtils.getNumberBetween(contentAsString, "=", ";for(");
            final String stringParameter = PlugUtils.getStringBetween(contentAsString, "='", "';for(");
            final String decodedLink = utf8Decode(enc(base64ToText(encodedLink), intParameter, stringParameter));
            final String finalURL = PlugUtils.getStringBetween(decodedLink, "href=\"", "\"");
            */
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("start download")
                    .toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkAllProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("404 Page Not Found") || contentAsString.contains("Sorry, the file you requested is not available") || contentAsString.contains("The page you are looking for is  not available")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("You cannot download more than one file at a time")) {
            throw new YouHaveToWaitException("You cannot download more than one file at a time", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "<h2 class=\"bgray\"><b>", "</b>");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</b>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String base64ToText(String t) {
        final String b64s = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_\"";
        String r = "";
        int m = 0;
        int a = 0;
        int c;

        for (int n = 0; n < t.length(); n++) {
            c = b64s.indexOf(t.charAt(n));

            if (c >= 0) {
                if (m > 0) {
                    r += (char) ((c << (8 - m)) & 255 | a);
                }

                a = c >> m;
                m += 2;

                if (m == 8) {
                    m = 0;
                }
            }
        }

        return r;
    }

    private String enc(String text, int intParameter, String stringParameter) {
        final int[] SGdo = new int[intParameter];
        final int WWL;
        WWL = intParameter;

        for (int UPYiAP = 0; UPYiAP < WWL; UPYiAP++) {
            SGdo[UPYiAP] = UPYiAP;
        }

        final String _RgfMb = stringParameter;
        int RNAcl = 0;
        final int[] lTBStY = SGdo;

        for (int LFEW_i = 0; LFEW_i < WWL; LFEW_i++) {
            RNAcl = (_RgfMb.charAt(LFEW_i % _RgfMb.length()) + lTBStY[LFEW_i] + RNAcl) % WWL;
            final int M_Aw = lTBStY[LFEW_i];
            lTBStY[LFEW_i] = lTBStY[RNAcl];
            lTBStY[RNAcl] = M_Aw;
            lTBStY[RNAcl] = lTBStY[RNAcl] ^ 5;
        }

        String SLIB = "";
        RNAcl = 0;

        for (int tzaVI = 0; tzaVI < text.length(); tzaVI++) {
            final int JOaTZ = tzaVI % WWL;
            RNAcl = (lTBStY[JOaTZ] + RNAcl) % WWL;
            final int ikBbi = lTBStY[JOaTZ];
            lTBStY[JOaTZ] = lTBStY[RNAcl];
            lTBStY[RNAcl] = ikBbi;
            SLIB += (char) ((text.charAt(tzaVI) ^ lTBStY[(lTBStY[JOaTZ] + lTBStY[RNAcl]) % WWL]));
        }

        return SLIB;
    }

    private String utf8Decode(String utfText) {
        String string = "";
        int i = 0;
        int c, c1, c2;

        while (i < utfText.length()) {
            c = utfText.charAt(i);

            if (c < 128) {
                string += (char) c;
                i++;
            } else if ((c > 191) && (c < 224)) {
                c1 = utfText.charAt(i + 1);
                string += (char) (((c & 31) << 6) | (c1 & 63));
                i += 2;
            } else {
                c1 = utfText.charAt(i + 1);
                c2 = utfText.charAt(i + 2);
                string += (char) (((c & 15) << 12) | ((c1 & 63) << 6) | (c2 & 63));
                i += 3;
            }
        }

        return string;
    }
}