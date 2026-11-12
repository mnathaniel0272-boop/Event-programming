// ============================================================
// FIRST INTERACTIVE BUTTON
// ============================================================
// This is the simplest interactive Swing application.
// When the user clicks the button, a message appears.
// ============================================================

import javax.swing.*;
import java.awt.*;

public class FirstButton {

public static void main(String[ ] args) {


SwingUtilities.invokeLater(FirstButton::createAndShow);
}

private static void createAndShow() {
JFrame frame = new JFrame("First Interactive Button");
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
frame.setSize(400, 200);
frame.setLocationRelativeTo(null);
frame.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 50));

// 1. Create the event source (the button)
JButton clickButton = new JButton("Click Me!");

// 2. Create the event listener (a lambda expression)
// The lambda implements ActionListener.actionPerformed()
clickButton.addActionListener(e -> {
// This code runs when the button is clicked
JOptionPane.showMessageDialog(frame,
"Successful execution!",
"Action executed",
JOptionPane.INFORMATION_MESSAGE);
});

frame.add(clickButton);
frame.setVisible(true);
}



}