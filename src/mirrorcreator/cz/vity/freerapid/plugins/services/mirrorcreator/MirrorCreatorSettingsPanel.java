package cz.vity.freerapid.plugins.services.mirrorcreator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
public class MirrorCreatorSettingsPanel extends JPanel {
    private MirrorCreatorSettingsConfig config;

    private final JCheckBox checkQueueAllLinks = new JCheckBox("Queue all links");

    public MirrorCreatorSettingsPanel(MirrorCreatorServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        checkQueueAllLinks.setSelected(config.isQueueAllLinks());
        checkQueueAllLinks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQueueAllLinks(checkQueueAllLinks.isSelected());
            }
        });
        setLayout(new BorderLayout());
        add(checkQueueAllLinks, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
