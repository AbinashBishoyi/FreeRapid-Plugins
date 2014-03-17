package cz.vity.freerapid.plugins.services.rapidshareuser;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.awt.*;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;



/**
 * @author Alex
 */
class RapidshareUserRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidshareUserRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        if (makeRedirectedRequest(method)) {
            String content = getContentAsString().toLowerCase();
            setClipboard (content);
//
//            Matcher matcher = PlugUtils.matcher("<title>(.+?)</title>", content);
//            //final Matcher matcher = getMatcherAgainstContent("<TITLE>(.+)</title>");
//            if (matcher.find()) {
//                String s = matcher.group(1);
//                 s = s.substring(1, s.length()-1);
//                logger.info("Link " + s);
//                try {
//
//
//                    this.httpFile.setNewURL(new URL(s));
//                } catch (MalformedURLException e) {
//                    throw new URLNotAvailableAnymoreException("Invalid URL");
//                }
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.COMPLETED );

//            } else {
//                checkProblems();
//                throw new PluginImplementationException();
//            }
//        } else {
//            checkProblems();
//            throw new ServiceConnectionProblemException();
        }
    }

    public static void setClipboard(String str) {
        StringSelection ss = new StringSelection(str);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }
    
    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }

}
