package cz.vity.freerapid.plugins.services.cryptit;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.cryptit.cypher.AESdecrypt;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class CryptItRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CryptItRunner.class.getName());

    private static final String HTTP_BASE = "http://crypt-it.com/";
    private static final String HTTP_ENGINE = HTTP_BASE + "/engine/";

    private static byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x11, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74,
            0x32, 0x2e, 0x67, 0x65, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x73, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x11,
            0x0a, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x06 };
    private static byte[] b2 = new byte[] { 0x02, 0x00, 0x00 };

    private static byte[] c = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x13, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74,
            0x32, 0x2e, 0x75, 0x70, 0x64, 0x61, 0x74, 0x65, 0x46, 0x69, 0x6c, 0x65, 0x00, 0x02, 0x2f, 0x32, 0x00, 0x00,
            0x00, 0x0f, 0x00, 0x00, 0x00, 0x01, 0x02, 0x00, 0x07 };

    private static final String cypherKey = "c281c3hOc1BLZk5TRERaSGF5cjMyNTIw";

    private List<URI> queye = new LinkedList<URI>();

    @Override
    @SuppressWarnings(value="deprecation")
    public void run() throws Exception {
        super.run();

        logger.info("Starting run task " + fileURL);
        Matcher matcher = PlugUtils.matcher("(?:http|ccf)://[.]*crypt-it.com/[s|e|d|c|a]/([a-zA-Z0-9]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Wrong Page !!!");
        }
        String cryptitcod = matcher.group(1);

        final PostMethod pMethod = getPostMethod(HTTP_ENGINE);
        pMethod.addRequestHeader("Content-Type", "application/x-amf");
        pMethod.setRequestBody(new String(b) + cryptitcod + new String(b2));

        InputStream icontent = client.makeRequestForFile(pMethod);
        String content = readFromInputStream(icontent);
        logger.fine(content);

        try {
            matcher = PlugUtils.matcher("url[^\\d\\w]*([\\d\\w]+)", content.toString());
            while (matcher.find()) {
                String url = AESdecrypt.decrypt(cypherKey, matcher.group(1));
                logger.finer( matcher.group(1) + "=" + url );
                queye.add(new URI(url));
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error:"+fileURL+"\n{"+content+"}", e);

            matcher = PlugUtils.matcher("http:[^$\\x00\\x04]*", content.toString());
            while (matcher.find()) {
                queye.add(new URI(matcher.group()));
            }
        }

        synchronized ( getPluginService().getPluginContext().getQueueSupport() ) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queye);
        }
        //downloadTask.sleep(2);
        //httpFile.setState(DownloadState.COMPLETED);
    }

    private String readFromInputStream(InputStream icontent) throws PluginImplementationException, IOException {
        if (icontent == null) {
            throw new PluginImplementationException("Can't load main page !!!");
        }
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(icontent));
        String line = null;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        if( content.length() == 0) {
            throw new PluginImplementationException("Can't locate any file !!!");
        }
        return content.toString();
    }

}
