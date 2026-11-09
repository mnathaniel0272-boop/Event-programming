import javax.swing.*;
import java.util.concurrent.ExecutionException;

public class Bug1_BlockingEDT extends JFrame {

    private JLabel statusLabel;
    private JButton processButton;

    public Bug1_BlockingEDT() {
        setTitle("Bug 1 - Fixed (EDT not blocked)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new java.awt.FlowLayout());

        statusLabel = new JLabel("Ready");
        processButton = new JButton("Process");

        processButton.addActionListener(e -> {
            // This handler runs on the EDT, so it must return quickly.
            // Update the UI immediately, then hand the slow work to a background thread.
            processButton.setEnabled(false);      // prevent clicking again while running
            statusLabel.setText("Processing...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Runs on a background thread. The slow work goes here.
                    // Never touch Swing components from this method.
                    Thread.sleep(5000);
                    return null;
                }

                @Override
                protected void done() {
                    // Runs back on the EDT, so it is safe to update the UI here.
                    try {
                        get();                    // rethrows any exception from doInBackground
                        statusLabel.setText("Done!");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        statusLabel.setText("Interrupted");
                    } catch (ExecutionException ex) {
                        statusLabel.setText("Error: " + ex.getCause().getMessage());
                    } finally {
                        processButton.setEnabled(true);
                    }
                }
            }.execute();
        });

        add(statusLabel);
        add(processButton);
        setSize(300, 150);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Bug1_BlockingEDT().setVisible(true);
        });
    }
}