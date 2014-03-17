package cz.vity.freerapid.plugins.services.shareonline;

import java.util.Date;
import java.util.logging.Logger;

/**
 * @author Ludek Zika
 */
class ServicePluginContext {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.shareonline.ServicePluginContext.class.getName());
    private Date startOfTicket = new Date(1L);

    ServicePluginContext() {
    }

    public Date getStartOfTicket() {
        return startOfTicket;
    }

    public void setStartOfTicket(Date startOfTicket) {
        logger.info("Setting startOfTicket to " + startOfTicket.toString());
        this.startOfTicket = startOfTicket;
    }
}