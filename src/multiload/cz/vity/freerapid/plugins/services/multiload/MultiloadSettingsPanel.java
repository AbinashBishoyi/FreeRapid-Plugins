package cz.vity.freerapid.plugins.services.multiload;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author JPEXS
 */
public class MultiloadSettingsPanel extends JPanel implements ActionListener {
    public static final String[] serverNames = {"multishare.cz","czshare.com","hellshare.com","share-rapid.cz","rapidshare.com","uloz.to","quickshare.cz"};
    private MultiloadSettingsConfig config;

    public MultiloadSettingsPanel(MultiloadServiceImpl service) throws Exception {
        super(new BorderLayout());
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred download shareserver:");
        final JComboBox serversList = new JComboBox(serverNames);
        serversList.setSelectedIndex((config.getServerSetting()+1)%7);
        serversList.addActionListener(this);
        add(qualityLabel, BorderLayout.PAGE_START);
        add(serversList, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    public void actionPerformed(ActionEvent e) {
        final JComboBox cb = (JComboBox) e.getSource();
        config.setServerSetting((cb.getSelectedIndex()-1)%7);
    }

}
