package cz.vity.freerapid.plugins.services.fdnlinks;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class FdnLinksFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FdnLinksFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final int i = fileURL.toLowerCase().indexOf("/link/");
        if (i == -1)
            throw new PluginImplementationException("Link not found");
        fileURL = "http://fdnlinks.com/ajax/links.html?a=" + fileURL.substring(i + "/link/".length()) + "&x=670";
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            parseWebsite();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
//        final String contentAsString = getContentAsString();
//        if (contentAsString.contains("File Not Found")) {
//            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
//        }
    }

    private void parseWebsite() throws URISyntaxException {
        final Matcher matcher = getMatcherAgainstContent("<link>(.+?)</link>");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();

        while (matcher.find(start)) {
            final String link = matcher.group(1);

            uriList.add(new URI(sixBitDecode(link)));

            start = matcher.end();
        }

        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private static String sixBitDecode(String s) {
        String a = "";
        String b = "";
        String c = "NOPQvwxyz012RSTUVWXYZabcdefghijkABCDEFGHIJKLMlmnopqrstu3456789_-";
        int _loc2;
        String d;
        for (int i = 0; i < s.length(); i++) {
            _loc2 = c.indexOf(s.substring(i, i + 1));
            String s1 = ("000000" + Integer.toBinaryString(_loc2));
            d = s1.substring(s1.length() - 5, s1.length());
            a = a + d;
        }
        for (int i = 0; i < a.length(); i = i + 8) {
            int i2 = i + 8;
            if (i2 > a.length()) {
                i2 = a.length();
            }
            d = a.substring(i, i2);
            b = b + (char) Integer.parseInt(d, 2);
        }
        return b.trim();
    }

}