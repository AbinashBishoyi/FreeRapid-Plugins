package cz.vity.freerapid.plugins.services.circlecaptcha;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author ntoskrnl
 */
class ClickLocationCaptchaPanel extends JPanel {

    private final CaptchaImageComponent captchaImageComponent;

    public ClickLocationCaptchaPanel(final Image image, final String message) {
        final JLabel labelMessage = new JLabel(message, JLabel.CENTER);
        captchaImageComponent = new CaptchaImageComponent(image);

        final GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(labelMessage)
                        .addComponent(captchaImageComponent)
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(labelMessage)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(captchaImageComponent)
        );
    }

    public Point getClickLocation() {
        return captchaImageComponent.getClickLocation();
    }

    private static class CaptchaImageComponent extends JComponent implements MouseListener {

        private final Image image;
        private Point clickLocation = null;

        public CaptchaImageComponent(final Image image) {
            this.image = image;
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            addMouseListener(this);
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D g = (Graphics2D) graphics.create();
            g.drawImage(image, 0, 0, null);
            if (clickLocation != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.RED);
                g.fillOval(clickLocation.x - 2, clickLocation.y - 2, 5, 5);
            }
            g.dispose();
        }

        public Point getClickLocation() {
            return clickLocation;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            clickLocation = e.getPoint();
            repaint();
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }

    }

}
