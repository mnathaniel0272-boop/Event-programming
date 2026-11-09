# Bug Fix Documentation

## Bug 1: Blocking the EDT
- **Symptom:** UI freezes for 5 seconds when the Process button is clicked. The window cannot repaint or respond, and "Done!" only appears after the freeze ends.
- **Cause:** The long operation (`Thread.sleep(5000)`) ran directly inside the button's `ActionListener`, which runs on the Event Dispatch Thread (EDT). While the EDT is busy, Swing cannot handle clicks or repaint the window.
- **Fix:** Run the operation in a background thread, update UI on EDT. A `SwingWorker` does both: the slow work runs in `doInBackground()` and the UI update runs in `done()`, which is called on the EDT.
- **Code Change:** Replaced the `Thread.sleep(5000)` in the click handler with a `SwingWorker<Void, Void>`. The handler now disables the button and sets the label to "Processing..." and then returns immediately. The sleep moved into `doInBackground()`. In `done()`, the code calls `get()` to surface any exception, sets the label to "Done!", and re-enables the button.

## Bug 2: Missing Listener
- **Symptom:** Clicking the Increment button does nothing.
- **Cause:** No `ActionListener` was registered on the button, so there was no event handler to respond to the click.
- **Fix:** Added an `ActionListener` to the increment button that increases the counter and updates the label.
- **Code Change:** Added this after the button is created:
  ```java
  incrementButton.addActionListener(e -> {
      count++;
      countLabel.setText("Count: " + count);
  });
  ```

## Bug 3: Wrong Thread
- **Symptom:** Erratic behavior and potential crashes or flickering, because the clock label was changed from outside the EDT.
- **Cause:** A plain background `Thread` called `timeLabel.setText(...)` in a loop. Swing components are not thread-safe and must only be updated on the EDT.
- **Fix:** Kept all GUI updates on the EDT. The template suggests `SwingUtilities.invokeLater()`, which would work here. I used a `javax.swing.Timer` instead, because it runs its action on the EDT automatically and removes the need for a thread, a loop, and `Thread.sleep`.
- **Code Change:** Removed the background thread, the `while (running)` loop, the `running` flag, and the `InterruptedException` handling. Added a `javax.swing.Timer` that fires every 1000 ms and sets the label text, with `setInitialDelay(0)` so the time shows right away. The Start button now calls `clockTimer.start()` and disables itself so the clock cannot be started twice.
  
  The `invokeLater` version of the fix would keep the thread and wrap only the label update:
  ```java
  SwingUtilities.invokeLater(() -> timeLabel.setText("Time: " + new Date()));
  ```

## Bug 4: Memory Leak
- **Symptom:** Memory usage grows over time as buttons are added and removed.
- **Cause:** "Remove All" removed the buttons from the panel (`buttonPanel.removeAll()`), but the `createdButtons` list still held a reference to every one of them. Java cannot garbage collect an object that is still referenced.
- **Fix:** Clear the list when removing buttons.
- **Code Change:** Added one line to the "Remove All" handler:
  ```java
  createdButtons.clear();
  ```
  Because the button labels use `createdButtons.size()`, the numbering now restarts at "Button 0" after a clear.