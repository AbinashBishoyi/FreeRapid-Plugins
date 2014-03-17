package cz.vity.freerapid.plugins.services.circlecaptcha;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.services.relink.captcha.CaptchaPreparer;
import org.jdesktop.application.Application;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {

    @Override
    protected void startup() {
        try {
            final int radiusMin = 12, radiusMax = 28, radiusIncrement = 1;
            final BufferedImage image = CaptchaPreparer.getPreparedImage(
                    ImageIO.read(new URL("http://www.relink.us/core/captcha/circlecaptcha.php")));
            JOptionPane.showConfirmDialog(null, new ImageIcon(image));
            final CircleHoughTransform c = new CircleHoughTransform(image, 0xFFFFFF, radiusMin, radiusMax, radiusIncrement);
            c.performHoughTransform();
            /*for (int r = radiusMin; r <= radiusMax; r += radiusIncrement) {
                JOptionPane.showConfirmDialog(null, new ImageIcon(c.generateHoughImage(r)));
            }*/
            //drawCircles(image, c.findCircles(0.8));
            drawCircle(image, c.findOpenCircle(0.8));
            JOptionPane.showConfirmDialog(null, new ImageIcon(image));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static void drawCircle(final BufferedImage image, final Circle c) {
        drawCircles(image, Arrays.asList(c));
    }

    private static void drawCircles(final BufferedImage image, final List<Circle> list) {
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.RED);
        for (final Circle c : list) {
            g.fillOval(c.x() - 1, c.y() - 1, 3, 3);
            g.drawOval(c.x() - c.r(), c.y() - c.r(), 2 * c.r(), 2 * c.r());
        }
        g.dispose();
    }

    public static void main(final String[] args) {
        Application.launch(TestApp.class, args);
    }

}