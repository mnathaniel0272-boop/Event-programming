import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Event-driven Parking Garage prototype (Java Swing).
 *
 * Event sources : 4 buttons, a list selection, and a javax.swing.Timer (system clock)
 * Handlers      : Car Arrives, Pay, Car Exits, Reset, Clock Tick, Selection Changed
 * All GUI work runs on the Event Dispatch Thread (EDT) via SwingUtilities.invokeLater().
 */
public class ParkingGarageApp extends JFrame {

    private static final int CAPACITY = 8;
    private static final double HOURLY_RATE = 2.50;

    // ---------- Model ----------
    private static class Ticket {
        final int id;
        final int entryHour;
        boolean paid;
        double feePaid;

        Ticket(int id, int entryHour) {
            this.id = id;
            this.entryHour = entryHour;
        }

        int hoursParked(int now) {
            return Math.max(1, now - entryHour); // minimum 1 hour
        }

        double feeDue(int now) {
            return hoursParked(now) * HOURLY_RATE;
        }
    }

    private final DefaultListModel<Ticket> tickets = new DefaultListModel<>();
    private int nextTicketId = 1;
    private int clockHour = 0;
    private double revenue = 0;

    // ---------- View ----------
    private final JLabel spacesLabel = new JLabel();
    private final JLabel clockLabel = new JLabel();
    private final JLabel revenueLabel = new JLabel();
    private final JLabel gateLabel = new JLabel("GATE: CLOSED", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ");
    private final JProgressBar occupancyBar = new JProgressBar(0, CAPACITY);
    private final JList<Ticket> ticketList = new JList<>(tickets);
    private final JTextArea log = new JTextArea(8, 30);

    private final JButton arriveBtn = new JButton("Car Arrives");
    private final JButton payBtn = new JButton("Pay Selected Ticket");
    private final JButton exitBtn = new JButton("Car Exits");
    private final JButton resetBtn = new JButton("Reset");

    public ParkingGarageApp() {
        super("Parking Garage - Event-Driven Prototype");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUi();
        registerHandlers();
        startClock();
        refreshDisplay();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Top: system state
        JPanel state = new JPanel(new GridLayout(0, 1, 2, 2));
        state.setBorder(BorderFactory.createTitledBorder("System State"));
        gateLabel.setOpaque(true);
        gateLabel.setFont(gateLabel.getFont().deriveFont(Font.BOLD, 16f));
        occupancyBar.setStringPainted(true);
        state.add(spacesLabel);
        state.add(occupancyBar);
        state.add(clockLabel);
        state.add(revenueLabel);
        state.add(gateLabel);
        state.add(statusLabel);
        add(state, BorderLayout.NORTH);

        // Center: active tickets
        ticketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i,
                                                          boolean sel, boolean focus) {
                Ticket t = (Ticket) v;
                String text = String.format("Ticket #%d | in at hour %d | %dh | due $%.2f | %s",
                        t.id, t.entryHour, t.hoursParked(clockHour), t.feeDue(clockHour),
                        t.paid ? "PAID" : "UNPAID");
                return super.getListCellRendererComponent(l, text, i, sel, focus);
            }
        });
        JScrollPane listScroll = new JScrollPane(ticketList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Cars in Garage (select a ticket)"));
        add(listScroll, BorderLayout.CENTER);

        // Bottom: controls + log
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        buttons.add(arriveBtn);
        buttons.add(payBtn);
        buttons.add(exitBtn);
        buttons.add(resetBtn);

        log.setEditable(false);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event Log"));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(logScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ---------- Event handlers ----------
    private void registerHandlers() {
        // Handler 1: VEHICLE_ARRIVED
        arriveBtn.addActionListener(this::onVehicleArrived);
        // Handler 2: PAYMENT_REQUESTED / PAYMENT_COMPLETED
        payBtn.addActionListener(this::onPayment);
        // Handler 3: VEHICLE_EXITING / EXIT_DENIED
        exitBtn.addActionListener(this::onVehicleExiting);
        // Handler 4: reset
        resetBtn.addActionListener(e -> onReset());
        // Handler 5: selection changed (updates status with the fee)
        ticketList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Ticket t = ticketList.getSelectedValue();
                setStatus(t == null ? " " : String.format("Ticket #%d: %s, amount due $%.2f",
                        t.id, t.paid ? "already paid" : "unpaid", t.paid ? 0.0 : t.feeDue(clockHour)));
            }
        });
    }

    private void onVehicleArrived(ActionEvent e) {
        logEvent("VEHICLE_ARRIVED");
        if (tickets.size() >= CAPACITY) {
            logEvent("GARAGE_FULL -> entry refused");
            setStatus("Garage is FULL. Entry refused.");
            flashGate("GATE: CLOSED (FULL)", Color.RED);
            return;
        }
        Ticket t = new Ticket(nextTicketId++, clockHour);
        tickets.addElement(t);
        logEvent("Ticket #" + t.id + " issued, entry gate opened");
        setStatus("Welcome! Ticket #" + t.id + " issued.");
        flashGate("GATE: OPEN (ENTRY)", new Color(0, 160, 0));
        refreshDisplay();
    }

    private void onPayment(ActionEvent e) {
        Ticket t = ticketList.getSelectedValue();
        logEvent("PAYMENT_REQUESTED");
        if (t == null) {
            setStatus("Select a ticket first.");
            return;
        }
        if (t.paid) {
            setStatus("Ticket #" + t.id + " is already paid.");
            return;
        }
        double fee = t.feeDue(clockHour);
        t.paid = true;
        t.feePaid = fee;
        revenue += fee;
        logEvent(String.format("PAYMENT_COMPLETED Ticket #%d $%.2f", t.id, fee));
        setStatus(String.format("Paid $%.2f for ticket #%d. You may exit.", fee, t.id));
        refreshDisplay();
    }

    private void onVehicleExiting(ActionEvent e) {
        Ticket t = ticketList.getSelectedValue();
        logEvent("VEHICLE_EXITING");
        if (t == null) {
            setStatus("Select the ticket of the exiting car.");
            return;
        }
        if (!t.paid) {
            logEvent("EXIT_DENIED Ticket #" + t.id + " unpaid");
            setStatus("Please pay first. Exit denied for ticket #" + t.id + ".");
            flashGate("GATE: CLOSED (UNPAID)", Color.RED);
            return;
        }
        tickets.removeElement(t);
        logEvent("Ticket #" + t.id + " exited, exit gate opened");
        setStatus("Thank you! Drive safely.");
        flashGate("GATE: OPEN (EXIT)", new Color(0, 160, 0));
        refreshDisplay();
    }

    private void onReset() {
        tickets.clear();
        nextTicketId = 1;
        clockHour = 0;
        revenue = 0;
        log.setText("");
        logEvent("SYSTEM_RESET");
        setStatus("System reset.");
        refreshDisplay();
    }

    // Handler 6: CLOCK_TICK - fired by a Timer, not by the user
    private void startClock() {
        new Timer(3000, e -> {
            clockHour++;
            logEvent("CLOCK_TICK -> hour " + clockHour);
            refreshDisplay();
        }).start();
    }

    // ---------- Display helpers (always called on the EDT) ----------
    private void refreshDisplay() {
        int used = tickets.size();
        spacesLabel.setText(String.format("Occupied: %d / %d   Free: %d", used, CAPACITY, CAPACITY - used));
        occupancyBar.setValue(used);
        occupancyBar.setString(used == CAPACITY ? "FULL" : used + " cars");
        clockLabel.setText("Simulated clock: hour " + clockHour + "  (advances every 3 s)");
        revenueLabel.setText(String.format("Revenue: $%.2f   Rate: $%.2f/hour", revenue, HOURLY_RATE));
        arriveBtn.setEnabled(used < CAPACITY);
        payBtn.setEnabled(used > 0);
        exitBtn.setEnabled(used > 0);
        ticketList.repaint(); // fees change as the clock advances
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void logEvent(String msg) {
        log.append(msg + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void flashGate(String text, Color color) {
        gateLabel.setText(text);
        gateLabel.setBackground(color);
        gateLabel.setForeground(Color.WHITE);
        Timer t = new Timer(1500, e -> {
            gateLabel.setText("GATE: CLOSED");
            gateLabel.setBackground(null);
            gateLabel.setForeground(null);
            logEvent("GATE_CLOSED");
        });
        t.setRepeats(false);
        t.start();
    }

    public static void main(String[] args) {
        // Build and show the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new ParkingGarageApp().setVisible(true));
    }
}