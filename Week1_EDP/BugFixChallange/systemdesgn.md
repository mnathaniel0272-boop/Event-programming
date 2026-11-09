# Event-Driven System Design

## System Name: Parking Garage

---

## Part 1: Event Sources
List all components that can generate events:

1. **Entry Gate Sensor / Ticket Dispenser** - Generates events when a vehicle arrives at the entrance and when a ticket is taken
2. **Payment Kiosk** - Generates events when a driver inserts a ticket, pays, or the payment fails
3. **Exit Gate Sensor** - Generates events when a vehicle reaches the exit and when it has fully passed through
4. **Space Occupancy Sensors** - Generate events when a space becomes occupied or free, and when the garage reaches capacity
5. **System Clock (Timer)** - Generates periodic tick events used to update parking duration, fees, and overstay warnings

---

## Part 2: Events
List all possible events in the system:

| Event Name | Source | Description |
|---|---|---|
| VEHICLE_ARRIVED | Entry Gate Sensor | A vehicle is detected at the entrance and a ticket is requested |
| GARAGE_FULL | Occupancy Sensors | Occupied spaces equal capacity; entry must be refused |
| PAYMENT_REQUESTED | Payment Kiosk | A driver presents a ticket and chooses to pay |
| PAYMENT_COMPLETED | Payment Kiosk | The fee was successfully paid; the ticket is marked paid |
| VEHICLE_EXITING | Exit Gate Sensor | A vehicle reaches the exit and presents a ticket |
| EXIT_DENIED | Exit Gate Sensor | The ticket has not been paid, so the gate stays closed |
| CLOCK_TICK | System Clock | One simulated hour passes; durations and fees are recalculated |
| GATE_CLOSED | Gate Controller (timer) | A gate automatically closes a short time after opening |

---

## Part 3: Event Handlers
For each event, describe what should happen:

| Event | Handler Action |
|---|---|
| VEHICLE_ARRIVED | If space is available: create a ticket with the entry time, increase occupancy, open the entry gate, and log it. Otherwise post GARAGE_FULL. |
| GARAGE_FULL | Show a "FULL" status on the display, keep the entry gate closed, and log the refusal. |
| PAYMENT_REQUESTED | Look up the ticket, calculate the fee (hours parked x hourly rate, minimum 1 hour), and show the amount due. |
| PAYMENT_COMPLETED | Mark the ticket as paid, add the fee to total revenue, and log it. |
| VEHICLE_EXITING | If the ticket is paid: open the exit gate, remove the ticket, decrease occupancy, and update the display. Otherwise post EXIT_DENIED. |
| EXIT_DENIED | Keep the exit gate closed, display "Please pay first", and log the attempt. |
| CLOCK_TICK | Advance the simulated clock by one hour and refresh the clock display and the fee shown on active tickets. |
| GATE_CLOSED | Set the gate status back to CLOSED. |

---

## Part 4: Event Flow Diagram

```
 +-------------+   +-------------+   +-------------+   +-------------+
 | Entry Gate  |   |   Payment   |   |  Exit Gate  |   | System Clock|
 |   Sensor    |   |    Kiosk    |   |   Sensor    |   |   (Timer)   |
 +------+------+   +------+------+   +------+------+   +------+------+
        |                 |                 |                 |
        v                 v                 v                 v
   +---------------------------------------------------------------+
   |                   Event Objects (ParkingEvent)                |
   |   type: VEHICLE_ARRIVED / PAYMENT_REQUESTED / CLOCK_TICK ...  |
   +-------------------------------+-------------------------------+
                                   |
                                   v
                      +------------------------+
                      |  Event Queue (EDT)     |
                      +------------+-----------+
                                   |
                                   v
                      +------------------------+
                      |   Event Loop (EDT)     |
                      |  picks next event      |
                      +------------+-----------+
                                   |
          +------------------------+-------------------------+
          v                        v                         v
   +--------------+        +---------------+         +---------------+
   | Entry Handler|        | Payment       |         | Exit Handler  |
   | (ticket,     |        | Handler       |         | (check paid,  |
   |  occupancy)  |        | (fee, revenue)|         |  open gate)   |
   +------+-------+        +-------+-------+         +-------+-------+
          |                        |                         |
          +------------------------+-------------------------+
                                   |
                                   v
                      +------------------------+
                      |  Garage State (model)  |
                      +------------+-----------+
                                   |
                                   v
                      +------------------------+
                      |  GUI Update            |
                      | (labels, list, log)    |
                      +------------------------+
```

In the Swing prototype: **button click -> ActionEvent -> event queue -> EDT event loop -> ActionListener (handler) -> update state -> update labels/list/log**. The Timer follows the same path, with the timer firing the ActionEvent instead of a user.

---

## Part 5: Potential Issues
What could go wrong with this system?

1. **Race condition on the last space** - Two vehicles arrive at the same moment and both get tickets, exceeding capacity. *Prevention:* Process events one at a time through a single queue (the EDT), or synchronize/lock the occupancy counter in a multi-threaded version, and re-check capacity inside the handler.
2. **Frozen GUI from slow handlers** - A slow operation (e.g., a payment gateway call) inside a handler blocks the EDT, so the display and gates stop responding. *Prevention:* Run slow work in a `SwingWorker` or background thread and return the result to the EDT with `SwingUtilities.invokeLater()`.
3. **Unpaid exit / ticket fraud** - A driver exits without paying, or reuses a ticket. *Prevention:* Always verify the `paid` flag in the exit handler and delete the ticket once used so it cannot be reused.
4. **Lost or duplicate events** - A sensor fires twice (bounce) or an event is dropped, so occupancy drifts from reality. *Prevention:* Debounce sensor input, give every ticket a unique ID, and periodically reconcile the counter with the real list of active tickets.
5. **Clock or state inconsistency** - The fee is computed from a stale time, so the driver is charged the wrong amount. *Prevention:* Store the entry time on the ticket and calculate the fee at payment time from a single trusted clock.