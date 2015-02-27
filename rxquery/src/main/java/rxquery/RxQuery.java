package rxquery;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 * A class that allows to execute queries with a thread lock,
 * register updatable queries and notify updatables with database change results.
 * All actions, notifications and updates occur on a background scheduler.
 * All results from the background scheduler will be delivered on a foreground scheduler.
 */
public class RxQuery {
    private final Scheduler backgroundScheduler;
    private final Scheduler foregroundScheduler;
    private final int debounceMs;

    private final SerializedSubject<String, String> bus = new SerializedSubject<>(PublishSubject.<String>create());
    private final Object lock = new Object();

    /**
     * @param backgroundScheduler a scheduler that will be used for background actions execution.
     * @param foregroundScheduler a scheduler that will be used for background actions result delivery.
     * @param debounceMs          amount of time in milliseconds to wait after a matching
     *                            notification before update an updatable.
     *                            See {@link rxquery.RxQuery#updatable}, {@link rx.Observable#debounce}
     */
    public RxQuery(Scheduler backgroundScheduler, Scheduler foregroundScheduler, int debounceMs) {
        this.backgroundScheduler = backgroundScheduler;
        this.foregroundScheduler = foregroundScheduler;
        this.debounceMs = debounceMs;
    }

    /**
     * Immediately executes a query in the current thread, performing a lock on a database.
     *
     * @param query a query to execute.
     * @param <R>   query result type.
     * @return an observable that will emit a query result.
     */
    public <R> Observable<R> query(final Func0<R> query) {
        return Observable.create(new Observable.OnSubscribe<R>() {
            @Override
            public void call(Subscriber<? super R> subscriber) {
                R result;
                synchronized (lock) {
                    result = query.call();
                }
                subscriber.onNext(result);
                subscriber.onCompleted();
            }
        });
    }

    /**
     * Creates an observable that executes a given query in the current thread immediately and
     * re-executes the query on a background scheduler in case of notifications that match a given regex.
     * The reason why first query in immediate is that user should not see how an empty screen blinks.
     *
     * @param query   a query to execute and to use for data updates
     * @param matches a data set to observe for updates
     * @param <R>     a type of returning data
     * @return an observable that returns query results
     */
    public <R> Observable<R> updatable(final Func0<R> query, DataPattern matches) {
        final Pattern pattern = matches.getPattern(); // benchmark results: compilation = 0.2 ms, matching = 0.1ms on a slow device
        return bus
            .filter(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return pattern.matcher(s).matches();
                }
            })
            .debounce(debounceMs, TimeUnit.MILLISECONDS, backgroundScheduler)
            .map(new Func1<String, R>() {
                @Override
                public R call(String s) {
                    synchronized (lock) {
                        return query.call();
                    }
                }
            })
            .observeOn(foregroundScheduler)
            .startWith(new Func0<R>() {
                @Override
                public R call() {
                    synchronized (lock) {
                        return query.call();
                    }
                }
            }.call());
    }

    /**
     * Executes an action on a background scheduler.
     * Notifies all subscribed updatables with returned data set after the execution but
     * no updates will be send on the null result.
     *
     * @param action an action to execute. The action must return a DataSet describing
     *               data changes. It is safe to return null in case
     *               of failed transaction.
     * @return an observable that needs to be subscribed to run the action
     */
    public Observable<Void> execution(final Func0<DataDescription> action) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                DataDescription result;
                synchronized (lock) {
                    result = action.call();
                }
                if (result != null)
                    bus.onNext(result.getDescription());
                subscriber.onCompleted();
            }
        }).subscribeOn(backgroundScheduler).observeOn(foregroundScheduler);
    }

    /**
     * Notifies updatables about indirect (around {@link rxquery.RxQuery#execution)} data change.
     *
     * @param result a data set describing data changes
     */
    public void notifyDataChange(DataDescription result) {
        bus.onNext(result.getDescription());
    }
}
