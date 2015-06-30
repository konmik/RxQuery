# RxQuery

RxQuery is a very simple (102 lines of code) library that adds RxJava functionality to your database layer.
This library is not limited by any ORM or direct SQL access. You can use ANY database kind you wish.
Queries in this library are not tied to tables, so you can can make multiple select statements and
return complex results.

## Introduction

The core of this library is an "updatable query" which returns an object of any query-specific result type.
You can trigger updatable queries by mentioning a table during a transaction execution.

All data modifications and query updates happen in a background thread, with automatic delivery
to the foreground thread. RxQuery creates a lock for every query, so
you will never get into a situation when your data has been changed by another
thread during successive database reads inside of one query.

An updatable query will run after a data change with some delay. This is done to handle a problem of overproducing observable,
see [debounce](https://github.com/ReactiveX/RxJava/wiki/Backpressure#debounce-or-throttlewithtimeout) RxJava operator.
You can set this delay manually depending on your application needs.

You can also use immediate (not updatable) queries in the main thread with RxQuery to get benefits of database locking and
reactive operators chaining.

## Example

This is how to register an updatable query which will run every time a change in "messages" or "notes" tables happens:

``` java
q.updatable(asList("notes", "messages"), new Func0<String>() {
    @Override
    public String call() {
        // your data access methods here
        return "Hello, world!";
    }
})
.subscribe(new Action1<String>() {
    @Override
    public void call(String queryResult) {
        Log.v("", queryResult);
    }
});
```

Output:

``` text
10:00 Hello, world!
```

This is an example of a data changing action. The action will be automatically called inside of a transaction
depending on the RxQuery configuration (see below). The call states that the action can alter data in "users" and "messages"
tables, so all `updatable`s that are subscribed to these tables will be automatically updated.

``` java
q.execution(asList("users", "messages"), new Action0<DataDescription>() {
    @Override
    public void call() {
        // some data change
    }
}).subscribe();
```

Output:

``` text
10:00 Hello, world!
10:05 Hello, world!
```

## Best practices

Make your queries return immutable objects. Immutable objects are safe to share across an application, especially if your
application is multi-threaded. When you're passing an immutable object you can be sure that your copy of the object
will never be changed. Use [AutoParcel](https://github.com/frankiesardo/auto-parcel) library, use
[SolidList](https://github.com/konmik/solid).
More on benefits of immutable objects you can read in the Joshua Bloch's "Effective Java" book and in the description of
[AutoValue](https://github.com/google/auto/tree/master/value) library.

Do not expose your database cursor or any other low-level data structure to the view layer of an application.
At first, such objects are not immutable. Second, this breaks layers of logic.
You can read about an effective way to architect your Android application here:
[Architecting Android... The clean way?](http://fernandocejas.com/2014/09/03/architecting-android-the-clean-way/)

The proper way to use this library is inside of a Presenter.
(You're already using [Model-View-Presenter](http://konmik.github.io/introduction-to-model-view-presenter-on-android.html), isn't it?)
Anyway, you can use the library without MVP, the entire library is is just three simple observables.

The entire pipeline with MVP would look like this (in a pseudocode).

Query some data from a database:

``` java
View:
    getPresenter().querySomeData();

Presenter.querySomeData:
    q
        .immediate({ model.readFromDatabase() })
        .subscribe({ getView().publishSomeData(data); });

Model.readFromDatabase:
    your sql / orm / etc calls
    return data
```

A data change:

``` java
View:
    getPresenter().changeSomeData(data);

Presenter.changeSomeData:
    q
        .execute(changedTableName, { model.changeSomeRows(data) })
        .subscribe();

Model.changeSomeRows:
    insert / delete / update
```

An updatable:

``` java
Presenter.onCreate:
    q
        .updatable(tableName, { model.readFromDatabase() })
        .subscribe({ getView().publishSomeData(data) }); // this part depends on your MVP library: there
                                                         // should be a method to delay onNext until a view becomes available.
```

## Installation

``` groovy
dependencies {
    compile 'info.android15.rxquery:rxquery:2.0.0'
}
```

## Initialization

``` java
RxQuery q = new RxQuery(
    Schedulers.from(Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "transactions");
        }
    })),
    AndroidSchedulers.mainThread(),
    150,
    new Action1<Action0>() {
        @Override
        public void call(final Action0 action0) {
            database.callInTransaction(action0);
        }
    }
)
```

## References

* [RxJava](https://github.com/ReactiveX/RxJava)
* [Solid](https://github.com/konmik/solid)
* [AutoParcel](https://github.com/frankiesardo/auto-parcel)
* [Nucleus](https://github.com/konmik/nucleus)
* [SQLite multithreading best practices](http://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android/3689883#3689883)
