package cz.vity.freerapid.plugins.services.fsx;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// If you want to make use of HtmlUnit (http://htmlunit.sourceforge.net/)
// you need the following imports too:
// import com.gargoylesoftware.htmlunit.BrowserVersion;
// import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
// import com.gargoylesoftware.htmlunit.WebClient;
// import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
// import com.gargoylesoftware.htmlunit.html.HtmlPage;
// import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * Class which contains main code
 *
 * @author Hosszu
 */
class FsxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FsxFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        // Do we need this?
        final GetMethod getMethod = getGetMethod(fileURL);          //make first request
        getMethod.addRequestHeader("Connection", "keep-alive");
        getMethod.removeRequestHeader("Keep-Alive");
        getMethod.addRequestHeader("Keep-Alive", "115");
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("The very first GET request failed.");
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (!content.contains("img/bg0.gif")) {
            // This link is corrupt, no file is available
            httpFile.setFileState(FileState.FILE_NOT_FOUND);
        } else if (content.contains("img/button-tovabbfree.gif")) {
            // Sorry name and size is at this stage not available
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            // <font color="#FF0000" size="4">Tgaags01e02hun.sfv</font></br>
            // <strong>FeltÃ¶ltve:</strong> 2009-08-22 22:13 <strong>MÃ©ret:</strong> 349 BÃ¡jt</font></br></br>
            PlugUtils.checkName(httpFile, content, "<font color=\"#FF0000\" size=\"4\">", "</font>");
            PlugUtils.checkFileSize(httpFile, content, "ret:</strong> ", " B");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        // The first GET loads a page where you have to choose between
        // free and premium download mode
        // <a href="download.php?i=1"><img src="img/button-tovabbfree.gif" width="100" height="23" border="0"></a>
        // You get a cookie which must be used throughout the session
        // After choosing the free download, you get a page which shows
        // how many users are waiting for a free thread.
        // This page must be updated as the HTTP header entry shows:
        // <META HTTP-EQUIV="Refresh" CONTENT="15; URL=download.php">
        // After each refresh you HAVE TO download "img/bg0.gif", otherwise
        // the refresh fails.
        // If there is your turn to download the file requested, the page refresh
        // submits the following entry:
        //  <a id='dlink' href="http://s4.fsx.hu/"><img src="img/button-letoltes2.gif" width="92" height="23" border="0" /></a>
        //  <script type="text/javascript">\n
        //   elem = document.getElementById("dlink");\n
        //   elem.href = elem.href + "h93v8kn9/77413464/Kemenykalap_es_krumpliorr.r22";\n     <= the file name in question
        //  </script></br></br>\n

        // Would it be easier to implement download with HtmlUnit?
        // See: http://htmlunit.sourceforge.net/
        // !!!!!!!!!!!!!!!!!!! ?????????????????????? processWithHtmlUnit();
        processWithFRDCalls();
    }

    public void processWithFRDCalls() throws Exception {
        final GetMethod method = getGetMethod(fileURL); //create GET request
        method.addRequestHeader("Connection", "keep-alive");
        method.removeRequestHeader("Keep-Alive");
        method.addRequestHeader("Keep-Alive", "115");
        if (makeRedirectedRequest(method)) { //we make the main request
            String contents = getContentAsString();//check for response
            checkProblems();             //check problems
            checkNameAndSize(contents);  //extract file name and size from the page

            Cookie sessionCookie = getCookieByName("PHPSESSID");
            addCookie(sessionCookie);

            final String baseURL = "http://" + method.getRequestHeader("Host").getValue();

            // Here is a method that downloads "img/bg0.gif" after each refresh
            // we need this otherwise the server doesn't answer correctly
            HttpMethod getBg0 = getMethodBuilder().setBaseURL(baseURL).setMethodAction("img/bg0.gif").toGetMethod();
            getBg0.addRequestHeader("Connection", "keep-alive");
            getBg0.removeRequestHeader("Keep-Alive");
            getBg0.addRequestHeader("Keep-Alive", "115");
            getBg0.removeRequestHeader("Accept");
            getBg0.addRequestHeader("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
            getBg0.removeRequestHeader("Referer");
            getBg0.addRequestHeader("Referer", baseURL + method.getPath() + "?" + method.getQueryString());
            makeRequest(getBg0);

            // At choosing the free download we have to submit "download.php?i=1"
            method.removeRequestHeader("Referer");
            method.addRequestHeader("Referer", baseURL + method.getPath() + "?" + method.getQueryString());
            method.setQueryString("i=1");

            if (makeRequest(method)) { //we make the main request for the free download
                contents = getContentAsString();//check for response
                // Now the name and the size is available
                checkNameAndSize(contents);
                logger.info(contents);
            }

            // The subsequent calls don't contain a query string and no caching
            // You can see this in the HTTP header:
            // <META HTTP-EQUIV="Refresh" CONTENT="15; URL=download.php">
            method.setQueryString("");
            method.addRequestHeader("Cache-Control", "max-age=0");

            getBg0.removeRequestHeader("Referer");
            getBg0.addRequestHeader("Referer", baseURL + method.getPath() + "?i=1");
            makeRequest(getBg0);

            // The subsequent calls are a bit others
            getBg0.removeRequestHeader("Referer");
            getBg0.addRequestHeader("Referer", baseURL + method.getPath());
            getBg0.addRequestHeader("Cache-Control", "max-age=0");

            // We need the GET of the button gif later, now prepare it
            HttpMethod getLe2 = getMethodBuilder().setBaseURL(baseURL).setMethodAction("img/button-letoltes2.gif").toGetMethod();
            getLe2.addRequestHeader("Connection", "keep-alive");
            getLe2.removeRequestHeader("Keep-Alive");
            getLe2.addRequestHeader("Keep-Alive", "115");
            getLe2.removeRequestHeader("Accept");
            getLe2.addRequestHeader("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
            getLe2.removeRequestHeader("Referer");
            getLe2.addRequestHeader("Referer", baseURL + method.getPath());
            getLe2.addRequestHeader("Cache-Control", "max-age=0");

            // Test: .....<strong>245</strong></font> felhaszn.....    The number of users waiting
            Pattern pat = Pattern.compile("\\<strong\\>(\\d+)\\<\\/strong\\>\\<\\/font\\> felhaszn");

            // Now wait until the Download button activates i.e. "button-letoltes2.gif" is available
            do {
                Matcher m = pat.matcher(contents);
                if (m.find()) {
                    String s = m.group(1);
                    logger.info("Users to wait for: " + s);
                } else {
                    throw new ServiceConnectionProblemException("Number of waiting users not found.");
                }
                // !!! The argument for sleep should be taken from the response header
                // there you see:
                // <META HTTP-EQUIV="Refresh" CONTENT="15; URL=download.php">
                Thread.sleep(15 * 1000);
                makeRequest(method);
                contents = getContentAsString();
                // Get the background gif image, to make happy the server
                makeRequest(getBg0);
                // We wait until the download will be set free, i. e. the download button is active
            } while (!contents.contains("button-letoltes2.gif"));

            logger.info(contents);

            // we have to GET the button image, otherwise the server responds with an error
            makeRequest(getLe2);

            // Extract the host and the file name from the contents, e.g.
            //  <a id='dlink' href="http://s4.fsx.hu/"><img src="img/button-letoltes2.gif" width="92" height="23" border="0" /></a>
            //  <script type="text/javascript">\n
            //   elem = document.getElementById("dlink");\n
            //   elem.href = elem.href + "h93v8kn9/77413464/Kemenykalap_es_krumpliorr.r22";\n     <= the file name in question
            pat = Pattern.compile("a id=\'dlink\' href=\"([^\"]*)\".*elem.href = elem.href.{1,5}\"([^\"]*)\"", Pattern.DOTALL);
            Matcher m = pat.matcher(contents);
            String fromHost;
            String getThis;
            if (m.find()) {
                // Get the two groups
                fromHost = m.group(1);
                getThis = m.group(2);
                logger.info("Download: " + getThis);
            } else {
                throw new ServiceConnectionProblemException("The host name or file name not found.");
            }

            final HttpMethod httpMethod = getMethodBuilder().setReferer(getBg0.getRequestHeader("Referer").
                    getValue()).setAction(getThis).toGetMethod();
            httpMethod.addRequestHeader("Host", fromHost);
            httpMethod.addRequestHeader("Connection", "keep-alive");
            httpMethod.removeRequestHeader("Keep-Alive");
            httpMethod.addRequestHeader("Keep-Alive", "115");
            httpMethod.removeRequestHeader("Content-Length");

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("The main request failed.");
        }
    }

    /*
        // Would it be easier to implement download with HtmlUnit?
        // See: http://htmlunit.sourceforge.net/
        // !!!!!!!!!!!!!!!!!!! ?????????????????????? processWithHtmlUnit();

    public void processWithHtmlUnit() throws Exception {

        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
        //WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_7);

        webClient.setRefreshHandler(new ThreadedRefreshHandler());
        //webClient.setRefreshHandler(new WaitingRefreshHandler());

        // This makes a JavaScript debugger console:     WebClientUtils.attachVisualDebugger(webClient);

        webClient.waitForBackgroundJavaScriptStartingBefore(20000);
        webClient.waitForBackgroundJavaScript(40000);

        HtmlPage currentPage = webClient.getPage(fileURL);
        logger.info("= 1 ==============================================================");
        System.out.println(currentPage.getWebResponse().getContentAsString());

        // how to look for anchors:
        // List<HtmlAnchor> myAnchors = currentPage.getAnchors();
        // Iterator<HtmlAnchor> itr = myAnchors.iterator();
        // HtmlAnchor currAnchor;
        // while (itr.hasNext()) {
        //     currAnchor = itr.next();
        //     System.out.println(currAnchor.toString());
        // }

        HtmlAnchor tovabbfree = currentPage.getAnchorByHref("download.php?i=1");
        currentPage = tovabbfree.click();
        logger.info("= 2 ==============================================================");
        System.out.println(currentPage.getWebResponse().getContentAsString());
        logger.info(currentPage.toString());

        webClient.waitForBackgroundJavaScript(15 * 1000);

        System.out.println(currentPage.getWebResponse().getWebRequest().toString());
        logger.info("= 3 ==============================================================");

        List<NameValuePair> requList = currentPage.getWebResponse().getWebRequest().getRequestParameters();
        Iterator<NameValuePair> ritr = requList.iterator();
        com.gargoylesoftware.htmlunit.util.NameValuePair relem;
        while (ritr.hasNext()) {
            relem = ritr.next();
            System.out.println(relem.getName() + " : " + relem.getValue() + "\n");
        }
        logger.info("= 4 ==============================================================");

        System.out.println(currentPage.getWebResponse().getWebRequest().getRequestParameters().toString());
        logger.info("= 5 ==============================================================");

        String cnt;
        String oldcnt = "";
        do {
            // !!!!!!!!!!!!!!!!!!! The argument for sleep should be taken from the response header
            Thread.sleep(15 * 1000);
            cnt = currentPage.getWebResponse().getContentAsString();
            if (cnt.equals(oldcnt)) {
                System.out.println("No change");
            }
            else {
                System.out.println("Page changed\n" + cnt + "\n");
                oldcnt = cnt;
            }
        } while (!cnt.contains("button-letoltes2.gif"));
    }
    */

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        // ??? How to test accented characters?
        if (contentAsString.contains("nem talÃ¡lhatÃ³")) {  // This string probably doesn't work - sorry // TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}

