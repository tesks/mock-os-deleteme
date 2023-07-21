/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.sys;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jpl.gds.shared.annotation.AlwaysThrows;


/**
 * Class Container.
 *
 * Immutable class to hold a reference as an object or null. Note that we
 * can cache for the null case. Derived from the Java 8 Optional class.
 *
 * Containers should pretty much NEVER be null themselves; instead they HOLD
 * a null. You can never get the null back out. Instead you check for it with
 * isPresent(). You should always call isPresent() before you try to get the
 * value out with get(). The exception that get() throws should never happen
 * unless you make a mistake. In other words, do not try to catch the
 * NoSuchElementException; make sure that it does not happen by checking
 * isPresent() first.
 *
 * There is only one empty Container. One is created for Object and then
 * cast to whatever type is needed. That works because of type erasure and
 * the fact that null is the same for all classes. However, do not rely on
 * the caching -- assume they are all separate objects and use isPresent().
 *
 * Since this class is such a workhorse class I tried to make it as fast as
 * possible, using the attributes directly instead of methods. But when it
 * gets to the functional methods I revert to the methods. (But I always use
 * the empty() method because of the cast.)
 *
 * Because this class was taken from Java 8 it cannot be exactly the same as
 * for Java 6. Java 6 interfaces do not have static or default. For
 * convenience the interfaces are in this file instead of broken out into
 * individual files.
 *
 * Container has been extended from Optional by making it serializable (if the
 * contents are) and Comparable (if the contents are.) A getRaw() has been
 * added. It is the equivalent of orElse(null).
 *
 * Container is serializable but is never actually serialized. Instead we
 * create a simple proxy that holds the reference. That proxy is serialized
 * but then uses the reference to create a new Container which is returned
 * instead. That will cause the empty value to remain cached.
 *
 *
 * @param <T> Type of reference
 */
public final class Container<T>
    extends Object implements Serializable, Comparable<Container<? extends T>>
{
    private static final long serialVersionUID = 0L;

    /** Single empty value for all T */
    private static final Container<Object> EMPTY_CONTAINER =
        new Container<Object>(null);

    /** Global string builder */
    private static final StringBuilder SB = new StringBuilder();

    /** The object of T we are holding or null */
    private final T _reference;

    /** True if not holding a null */
    private final boolean _nonEmpty;

    /**
     * The object as a Comparable of the T we are holding or null.
     * The atomic is most likely not strictly necessary.
     */
    private final AtomicReference<Comparable<T>> _comparable =
        new AtomicReference<Comparable<T>>(null);

    /** True if converted to Comparable */
    private final AtomicBoolean _converted = new AtomicBoolean(false);


    /**
     * Private constructor.
     *
     * @param reference Reference to be held (may be null)
     */
    private Container(final T reference)
    {
        super();

        _reference = reference;
        _nonEmpty  = (reference != null);
    }


    /**
     * Return empty Container. Note that we always return the same one.
     * But do not assume that; do not use == to compare for empty.
     * The cast is necessary if we want to cache.
     *
     * @return Cached empty Container
     *
     * @param <T> Type of reference
     */
    public static <T> Container<T> empty()
    {
        return SystemUtilities.<Container<T>>castNoWarning(EMPTY_CONTAINER);
    }


    /**
     * Return Container holding reference.
     *
     * @param reference Non-null reference
     *
     * @return Container holding reference
     *
     * @param <T> Type of reference
     */
    public static <T> Container<T> of(final T reference)
    {
        if (reference == null)
        {
            throw new IllegalArgumentException("Container.of");
        }

        return new Container<T>(reference);
    }


    /**
     * Return Container holding reference.
     *
     * @param reference Reference which may be null
     *
     * @return Container holding reference
     *
     * @param <T> Type of reference
     */
    public static <T> Container<T> ofNullable(final T reference)
    {
        return ((reference != null)
                    ? new Container<T>(reference)
                    : Container.<T>empty());
    }


    /**
     * Return the reference if there is one. Throw if empty.
     *
     * @return Reference
     */
    public T get()
    {
        if (! _nonEmpty)
        {
            throw new NoSuchElementException("Container.get");
        }

        return _reference;
    }


    /**
     * Return the reference if there is one. Does not
     * throw if empty.
     *
     * @return Reference
     */
    public T getRaw()
    {
        return _reference;
    }


    /**
     * Return the reference if there is one, else the other.
     *
     * @param other Alternative reference (or null)
     *
     * @return Reference or other
     */
    public T orElse(final T other)
    {
        return (_nonEmpty ? _reference : other);
    }


    /**
     * Do we have a value?
     *
     * @return True if not empty
     */
    public boolean isPresent()
    {
        return _nonEmpty;
    }


    /**
     * Get hash code. We cannot pre-compute because the hash code of
     * the immutable reference could change.
     *
     * @return Hash code of reference or zero
     */
    @Override
    public int hashCode()
    {
        return (_nonEmpty ? _reference.hashCode() : 0);
    }


    /**
     * Compare this Container against another object. We specifically handle
     * nulls to be compatible with compareTo
     *
     * @param o The other object
     *
     * @return True if equal
     */
    @Override
    public boolean equals(final Object o)
    {
        if (o == this)
        {
            return true;
        }

        // A Container should not be null, so we should throw on a null one.
        // But equals must return false on a null.

        if ((o == null) || ! o.getClass().equals(Container.class))
        {
            return false;
        }

        final Container<?> other = Container.class.cast(o);

        if (_reference == other._reference)
        {
            // Is same object or both null

            return true;
        }

        // Not both null at this point

        if (! _nonEmpty || ! other._nonEmpty)
        {
            // Either null

            return false;
        }

        return _reference.equals(other._reference);
    }


    /**
     * Compare this Container against another Container. Will throw
     * ClassCastException if this object is not Comparable. Empty values are
     * handled here; otherwise, we use compareTo of T.
     *
     * NB: We compare nulls here, although compareTo does not usually work with
     * nulls. That is because having a null is normal for a Container. We make
     * null less than any value.
     *
     * We want to throw if T is not Comparable, and we also want to save a
     * version of _reference cast to Comparable to speed up the comparison.
     * To do that we need a synchronized block, but we want to avoid the
     * synchronization as much as we can. So we use an atomic to tell us that
     * the cast has been performed already, and only enter the synchronized
     * block if it has not been. Once in the block we must double-check the
     * flag. Then we try to cast to Comparable and save the result if it works.
     * If it doesn't we leave _comparable null as a flag.
     *
     * Now we can throw if T is not Comparable. Note that we can throw many
     * times if we continue to be called, but we only have to determine that
     * once.
     *
     * @param o The other Container
     *
     * @return Compare status
     */
    @Override
    public int compareTo(final Container<? extends T> o)
    {
        if (! _nonEmpty)
        {
            return (o._nonEmpty ? -1 : 0);
        }

        if (! o._nonEmpty)
        {
            return 1;
        }

        // No nulls past this point

        if (! _converted.get())
        {
            synchronized (this)
            {
                if (! _converted.get())
                {
                    try
                    {
                        _comparable.set(
                            SystemUtilities.<Comparable<T>>castNoWarning(
                                Comparable.class.cast(_reference)));
                    }
                    catch (final ClassCastException cce)
                    {
                        _comparable.set(null);
                    }

                    _converted.set(true);
                }
            }
        }

        final Comparable<T> local = _comparable.get();

        if (local == null)
        {
            // We are not Comparable

            throw new ClassCastException("Container.compareTo");
        }

        if (_reference == o._reference)
        {
            // Same reference object

            return 0;
        }

        return local.compareTo(o._reference);
    }


    /**
     * Turn to a string. We cannot pre-compute.
     *
     * @return Object as a string
     */
    @Override
    public String toString()
    {
        if (! _nonEmpty)
        {
            return "Empty Container";
        }

        synchronized (SB)
        {
            SB.setLength(0);

            SB.append("Container(").append(_reference).append(')');

            return SB.toString();
        }
    }


    // From here on down I avoid exposing internals and use methods.


    /**
     * Filter against predicate.
     *
     * @param predicate Predicate object
     *
     * @return Result
     */
    public Container<T> filter(final Predicate<? super T> predicate)
    {
        return ((isPresent() && predicate.test(get()))
                    ? this
                    : Container.<T>empty());
    }


    /**
     * Map against function that returns Container.
     *
     * @param mapper Mapper object
     *
     * @return Result
     *
     * @param <U> Result type
     */
    public <U> Container<U> flatMap(
        final Function<? super T, Container<U>> mapper)
    {
        if (! isPresent())
        {
            return Container.<U>empty();
        }

        final Container<U> result = mapper.apply(get());

        if (result == null)
        {
            throw new IllegalArgumentException("Container.flatMap");
        }

        return result;
    }


    /**
     * Map against function.
     *
     * @param mapper Mapper object
     *
     * @return Result
     *
     * @param <U> Result type
     */
    public <U> Container<U> map(final Function<? super T, ? extends U> mapper)
    {
        return (isPresent() ? Container.<U>ofNullable(mapper.apply(get()))
                            : Container.<U>empty());
    }


    /**
     * Consume value if present.
     *
     * @param consumer Consumer object
     */
    public void ifPresent(final Consumer<? super T> consumer)
    {
        if (isPresent())
        {
            consumer.accept(get());
        }
    }


    /**
     * Return our value if present, otherwise get from supplier.
     *
     * @param supplier Supplier object
     *
     * @return Result
     */
    public T orElseGet(final Supplier<? extends T> supplier)
    {
        return (isPresent() ? get() : supplier.get());
    }


    /**
     * Return our value if present, otherwise throw (from supplier).
     *
     * @param <X> Exception type
     * @param supplier Supplier object
     *
     * @return Result
     *
     * @throws X If not present
     *
     */
    public <X extends Throwable> T orElseThrow(
        final Supplier<? extends X> supplier) throws X
    {
        if (! isPresent())
        {
            throw supplier.get();
        }

        return get();
    }


    /**
     * This is here just in case it is called by reflection.
     *
     * @return Object Cloned object
     *
     * @throws CloneNotSupportedException Always
     */
    @Override
    @AlwaysThrows
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Container.clone");
    }


    /**
     * Proxy to serialize instead of us.
     *
     * @return Proxy
     */
    private Object writeReplace()
    {
        return new ContainerProxy(_reference);
    }


    /**
     * Predicate interface.
     *
     * @param <T> Reference type
     */
    public static interface Predicate<T>
    {
        /**
         * And.
         *
         * @param other Other
         *
         * @return Result predicate
         */
        public Predicate<T> and(final Predicate<? super T> other);


        /**
         * Or.
         *
         * @param other Other
         *
         * @return Result predicate
         */
        public Predicate<T> or(final Predicate<? super T> other);


        /**
         * Negate.
         *
         * @return Result predicate
         */
        public Predicate<T> negate();


        /**
         * Test value against predicate.
         *
         * @param t Value
         *
         * @return True if matches predicate
         */
        public boolean test(final T t);


        /**
         * Return predicate that compares with equals.
         *
         * @param targetRef Other object
         *
         * @return Result predicate
         */
        public Predicate<T> isEqual(final Object targetRef);
    }


    /**
     * Function interface.
     *
     * @param <T> Reference type
     * @param <R> Result type
     */
    public static interface Function<T, R>
    {
        /**
         * Apply function to argument.
         *
         * @param t Argument
         *
         * @return Result
         */
        public R apply(final T t);


        /**
         * And then.
         *
         * @param after After function
         *
         * @return Result function
         *
         * @param <V> Result
         */
        public <V> Function<T, V> andThen(
            final Function<? super R, ? extends V> after);


        /**
         * Before.
         *
         * @param before Before function
         *
         * @return Result function
         *
         * @param <V> Result
         */
        public <V> Function<V, R> compose(
            final Function<? super V, ? extends T> before);


        /**
         * Function that returns its own argument.
         *
         * @return Result function
         *
         * @param <TT> Result
         */
        public <TT> Function<TT, TT> identity();
    }


    /**
     * Consumer interface.
     *
     * @param <T> Reference type
     */
    public static interface Consumer<T>
    {
        /**
         * Perform operation on argument.
         *
         * @param t Argument
         */
        public void accept(final T t);


        /**
         * Make composed consumer.
         *
         * @param after After function
         *
         * @return Result function
         */
        public Consumer<T> andThen(final Consumer<? super T> after);
    }


    /**
     * Supplier interface.
     *
     * @param <T> Reference type
     */
    public static interface Supplier<T>
    {
        /**
         * Get value.
         *
         * @return Value
         */
        public T get();
    }


    /**
     * This class is a proxy for the main class. It must be static.
     */
    private static final class ContainerProxy
        extends Object implements Serializable
    {
        private static final long serialVersionUID = 0L;

        private final Object _reference;


        /**
         * Constructor.
         * @param reference object referenced by the container
         */
        public ContainerProxy(final Object reference)
        {
            super();

            _reference = reference;
        }

        /**
         * Called to load a copy of the main class. Must be done for empty so
         * we can get the cached value.
         *
         * @return Container
         */
        private Object readResolve()
        {
            return Container.ofNullable(_reference);
        }
    }


    /**
     * Main method as an example.
     *
     * @param args Arguments
     */
    public static void main(final String[] args)
    {
        final Container<String> s = Container.of("Hello World");

        s.ifPresent(new Consumer<String>()
                    {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void accept(final String s)
                        {
                            System.out.println(s);
                        }


                        /**
                         * {@inheritDoc}
                         *
                         * We need to define this because Java 6 does not
                         * accept default in interfaces.
                         */
                        @Override
                        public Consumer<String> andThen(
                            final Consumer<? super String> after)
                        {
                            return null;
                        }
                    });
    }
}
