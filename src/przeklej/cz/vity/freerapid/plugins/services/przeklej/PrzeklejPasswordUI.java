package cz.vity.freerapid.plugins.services.przeklej;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * Author: Eterad
 * Date: 2010-01-29
 * Time: 12:04:29
 */
class PrzeklejPasswordUI extends JPanel{

    /*
     *  Creates new form PrzeklejPasswordUI
     */

    public PrzeklejPasswordUI() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        passField = new JTextField();
        jLabel1   = new JLabel();
        jLabel2   = new JLabel();
        jLabel1.setText("Enter password:");
        jLabel2.setFont(new Font("Tahoma", 1, 11));
        jLabel2.setText("This file is secured with password!");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(passField, GroupLayout.PREFERRED_SIZE, 118, GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(37, Short.MAX_VALUE)
                )
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap(16, Short.MAX_VALUE)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel1)
                                .addComponent(passField, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(19, 19, 19)
                )
        );
    }

    // Variables - do not change
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JTextField passField;
    // End of variables

    public String getPassword() {
        return passField.getText();
    }

}
