import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ListenerDemo {

// ============================================================
// TECHNIQUE 1: A NAMED CLASS (reusable, but verbose)
// ============================================================
static class NamedClassListener implements ActionListener {
@Override
public void actionPerformed(ActionEvent e) {
System.out.println("Style 1: Named class listener fired!");
}
}

public static void main(String[] args) {
SwingUtilities.invokeLater(ListenerDemo::createAndShow);
}

private static void createAndShow() {


JFrame frame = new JFrame("Three Listener Techniques");
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
frame.setSize(500, 300);
frame.setLocationRelativeTo(null);
frame.setLayout(new GridLayout(4, 1, 10, 10));

// Style 1: Named class
JButton btn1 = new JButton("Style 1: Named Class");
btn1.addActionListener(new NamedClassListener());
frame.add(btn1);

// --------------------------------------------------------
// TECHNIQUE 2: ANONYMOUS INNER CLASS (verbose, common in
// pre-Java 8 code)
// --------------------------------------------------------
JButton btn2 = new JButton("Style 2: Anonymous Inner Class");
btn2.addActionListener(new ActionListener() {
@Override
public void actionPerformed(ActionEvent e) {
System.out.println("Style 2: Anonymous inner class fired!");
}
});
frame.add(btn2);

// --------------------------------------------------------
// TECHNIQUE 3: LAMBDA EXPRESSION (concise, preferred in



// modern Java)
// --------------------------------------------------------
JButton btn3 = new JButton("Style 3: Lambda");
btn3.addActionListener(e -> {
System.out.println("Style 3: Lambda expression fired!");
});
frame.add(btn3);

// --------------------------------------------------------
// BONUS TECHNIQUE: METHOD REFERENCE (concise alternative)
// --------------------------------------------------------
JButton btn4 = new JButton("Style 4: Method Reference");
btn4.addActionListener(ListenerDemo::handleButtonClick);
frame.add(btn4);

frame.setVisible(true);
}

// Helper method referenced by btn4
private static void handleButtonClick(ActionEvent e) {
System.out.println("Style 4: Method reference fired!");
}
}