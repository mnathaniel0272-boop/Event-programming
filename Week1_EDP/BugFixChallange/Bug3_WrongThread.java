import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class Bug3_WrongThread extends JFrame {

    private JLabel timeLabel;
    private JButton startButton;
    private Timer clockTimer;

    public Bug3_WrongThread() {
        setTitle("Bug 3 - Fixed (Swing Timer)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        timeLabel = new JLabel("Time: ");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        startButton = new JButton("Start Clock");

        // FIX: javax.swing.Timer fires its action on the EDT,
        // so it is safe to update the label directly inside it.
        clockTimer = new Timer(1000, e -> timeLabel.setText("Time: " + new Date()));
        clockTimer.setInitialDelay(0);   // show the time immediately on start

        startButton.addActionListener(e -> {
            clockTimer.start();
            startButton.setEnabled(false);   // prevents starting the clock twice
        });

        add(timeLabel);
        add(startButton);
        setSize(350, 150);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Bug3_WrongThread().setVisible(true);
        });
    }
}