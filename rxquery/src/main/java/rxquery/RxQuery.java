package rxquery;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static java.util.Collections.singletonList;

/**
 * A class that allows to execute queries with a thread lock,
 * registers updatable queries and notifies updatables with database change events.
 * All executions, notifications and updates occur on the background scheduler.
 * All results from the background scheduler will be delivered on the foreground scheduler.
 */
public final class RxQuery {
    private final Scheduler backgroundScheduler;
    private final Scheduler foregroundScheduler;
    private final Action1<Action0> executor;
    private final int debounceMs;

    private final PublishSubject<String> updates = PublishSubject.create();
    private final Object lock = new Object();

    /**
     * @param backgroundScheduler a scheduler that will be used for background actions execution.
     * @param foregroundScheduler a scheduler that will be used for background actions result delivery.
     * @param debounceMs          amount of time in milliseconds to wait after a matching
     *                            table change before updating an updatable.
     *                            See {@link #updatable}, {@link rx.Observable#debounce}
     * @param executor            an executor for {@link #execution} calls. Use it to
     *                            supply transaction executor that is specific to the database api.
     */
    public RxQuery(Scheduler backgroundScheduler, Scheduler foregroundScheduler, int debounceMs, Action1<Action0> executor) {
        this.backgroundScheduler = backgroundScheduler;
        this.foregroundScheduler = foregroundScheduler;
        this.debounceMs = debounceMs;
        this.executor = executor;
    }

    /**
     * Immediately executes a query on the current thread, performing a lock on the database.
     *
     * @param query a query to execute.
     * @param <R>   query result type.
     * @return an observable that will emit a query result.
     */
    public <R> Observable<R> immediate(final Func0<R> query) {
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
     * re-executes the query on the background scheduler in case of a given tables change.
     * The reason why first query is immediate is that a user should not see an empty screen
     * in case if the screen transition is immediate.
     *
     * @param <R>    a type of returning data
     * @param tables a table list to observe for updates
     * @param query  a query to execute and to use for data updates
     * @return an observable that returns query results
     */
    public <R> Observable<R> updatable(final Iterable<String> tables, final Func0<R> query) {
        return updates
            .filter(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String it) {
                    for (String table : tables)
                        if (table.equals(it))
                            return Boolean.TRUE;
                    return Boolean.FALSE;
                }
            })
            .observeOn(backgroundScheduler)
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
     * A shortcut for {@link #updatable(Iterable, Func0)} with one table.
     */
    public <R> Observable<R> updatable(String table, Func0<R> query) {
        return updatable(singletonList(table), query);
    }

    /**
     * Executes an action on a background scheduler.
     * Notifies all subscribed updatables with data changes on given tables.
     *
     * @param tables tables that may be changed by the execution.
     * @param action an action to execute.
     * @return an observable that needs to be subscribed to run the action
     */
    public Observable<Void> execution(final Iterable<String> tables, final Action0 action) {
        return Observable
            .create(new Observable.OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> subscriber) {
                    synchronized (lock) {
                        executor.call(action);
                    }
                    for (String table : tables)
                        updates.onNext(table);
                    subscriber.onCompleted();
                }
            })
            .subscribeOn(backgroundScheduler)
            .observeOn(foregroundScheduler);
    }

    /**
     * A shortcut for {@link #execution(Iterable, Action0)} with one table.
     */
    public Observable<Void> execution(String table, Action0 action) {
        return execution(singletonList(table), action);
    }
}
