# Limitations of java.util.Observer and java.util.Observable

## Limitation 1: Single Inheritance
Explain: Why is it a problem that Observable is a CLASS, not an INTERFACE?

Your answer: To become a subject, a class must `extend Observable`. Java allows
only one superclass, so any class that already extends something else (for
example `WeatherStation extends Sensor`) cannot also be observable. You would
have to use composition (hold an Observable inside and delegate), which is
clumsy. Worse, `setChanged()` is `protected`, so code outside the subclass
cannot call it on a helper Observable. An interface such as `Observer` could be
implemented by any class, regardless of its inheritance chain.

## Limitation 2: Cannot Change the Notification Order
Explain: In what order are observers notified? Can you control this?

Your answer: The Javadoc says the order is unspecified, and applications should
not depend on it. In the actual implementation, observers are stored in a
`Vector` and notified in reverse order of registration (last added, first
notified). You cannot control this: there is no priority parameter and no
ordering hook. If one display must update before another (for example, a
calculation display before a chart that uses it), the API gives you no
reliable way to guarantee it.

## Limitation 3: setChanged() is Confusing
Explain: Why must you call setChanged() before notifyObservers()?

Your answer: `notifyObservers()` only does anything if the internal `changed`
flag is true, and `setChanged()` is what sets it. The idea was to let a subject
batch several updates and notify only once, or skip notifying when nothing
meaningful changed. In practice it is an extra step that is easy to forget. If
you omit it, the code compiles and runs with no error, but observers are
silently never notified, which makes the bug hard to find. The flag is also
cleared after each notification, so it must be set again every time.

## Limitation 4: Pull Model by Default
Explain: How does the observer get the data? Is this efficient?

Your answer: `notifyObservers()` with no argument passes `null` as `arg`, so the
observer receives only a reference to the Observable and must call getters
(`getTemperature()`, `getHumidity()`) to fetch the data. It also needs an
`instanceof` check and a cast, which is not type-safe. This is less efficient
and more error-prone than a push model where the data arrives as a typed
parameter. An observer interested in only one value still has to make extra
calls to find out what changed. (A push is possible via
`notifyObservers(Object arg)`, but `arg` is a plain `Object`, so it needs
casting too.)

## Limitation 5: Deprecated Since Java 9
Explain: Why did Java deprecate these classes?

Your answer: They were deprecated in Java 9 because the design has several
flaws. Observable is a class rather than an interface (see Limitation 1).
Notification order is unspecified (Limitation 2). `setChanged()` is easy to
forget (Limitation 3). Observers must pull data in an untyped way (Limitation
4). The API is not thread-safe or extensible: events are not serializable, and
the design does not support rich event types. The JDK team recommended using
`java.beans.PropertyChangeListener` / `PropertyChangeSupport`, the
`java.util.concurrent.Flow` API (reactive streams), or a custom observer
interface instead. Writing your own `Subject`/`Observer` interfaces, as in the
rest of this week's activities, avoids all of these problems.
