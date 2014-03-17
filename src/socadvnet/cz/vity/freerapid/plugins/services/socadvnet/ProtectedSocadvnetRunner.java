package cz.vity.freerapid.plugins.services.socadvnet;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
public class ProtectedSocadvnetRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ProtectedSocadvnetRunner.class.getName());
    private static Cookie[] cookies = null;
    private List<URI> queye = new LinkedList<URI>();

	@Override
	public void run() throws Exception {
        super.run();
        if( cookies != null && cookies.length > 0 ) {
            client.getHTTPClient().getState().addCookies(cookies);
        }
        logger.info("Starting run task " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        logger.info(fileURL);

        if (makeRequest(getMethod)) {
            //System.out.println( getContentAsString() );

            runCaptcha();
            final HttpMethod httpMethod = getMethodBuilder()
                .setAction("http://protected.socadvnet.com/allinks.php")
                .setParameter("LinkName", fileURL.substring(fileURL.indexOf('?')+1) )
                .setReferer( fileURL )
                .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException();

            String[] links = getContentAsString().split("\\|");

            for( int i=0;i<links.length;i++ ) {
                final HttpMethod httpMethod2 = getMethodBuilder()
                    .setAction("http://protected.socadvnet.com/allinks.php")
                    .setParameter("out_name", fileURL.substring(fileURL.indexOf('?')+1) )
                    .setParameter("link_id", String.valueOf(i) )
                    .setReferer( fileURL )
                    .toGetMethod();
                if (!makeRedirectedRequest(httpMethod2))
                    throw new ServiceConnectionProblemException();

                queye.add(new URI(PlugUtils.getStringBetween(getContentAsString(), ";url=", "\"")));
            }


            synchronized ( getPluginService().getPluginContext().getQueueSupport() ) {
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queye);
            }
        } else {
        	throw new ServiceConnectionProblemException();
        }
	}

	private void runCaptcha() throws Exception {
	    try {
	        String expr = PlugUtils.getStringBetween(getContentAsString(), "<div id =\"cp\">", "</div>");
	        //check = check.replaceAll("<[^>]*>", "").replaceAll("&[^;]{4};", "");
	        expr = expr.substring(0,expr.indexOf("=")-1);
	        int result = eval(expr);
	        final HttpMethod httpMethod = getMethodBuilder()
	            .setAction("http://protected.socadvnet.com/cp_code.php")
	            .setParameter("res_code", String.valueOf(result) )
	            .setReferer( fileURL )
	            .toPostMethod();
	        if (!makeRedirectedRequest(httpMethod))
	            throw new ServiceConnectionProblemException();
	        if( !getContentAsString().equals("1") )
	            throw new ServiceConnectionProblemException();

	        cookies = client.getHTTPClient().getState().getCookies();
	    }catch(PluginImplementationException e) {
	    }

	}

    public static int eval(String expr) throws ServiceConnectionProblemException {
        Matcher matcher = PlugUtils.matcher("(\\d+)\\s?([\\+\\-])\\s?(\\d+)", expr);
        int result;
        if(matcher.find() && matcher.groupCount() == 3) {
            if( matcher.group(2).equals("-") )
                result = Integer.parseInt(matcher.group(1)) - Integer.parseInt(matcher.group(3));
            else if( matcher.group(2).equals("+") )
                result = Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(3));
            else if( matcher.group(2).equals("/") )
                result = Integer.parseInt(matcher.group(1)) / Integer.parseInt(matcher.group(3));
            else if( matcher.group(2).equals("*") )
                result = Integer.parseInt(matcher.group(1)) * Integer.parseInt(matcher.group(3));
            else
                throw new ServiceConnectionProblemException();
        } else {
            throw new ServiceConnectionProblemException();
        }
        logger.info("Expression: " + expr + " = " + result );
        return result;
    }


}
