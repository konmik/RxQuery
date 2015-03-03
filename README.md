
# RxQuery

RxQuery is a very simple (150 lines of code) library that adds RxJava functionality to your database layer.
This library is not limited by any ORM or direct SQL access. You can use ANY database type you wish.

## Introduction

The core of this library is an "updatable query" which returns an object of any query-specific result type.
You can trigger your updatable queries by a table change or by a change in a specific data row,
or you can even create your own triggering rules.

Data modification and query updates happen in a background thread, with automatic delivery
to the foreground thread. RxQuery creates a lock for every query, so
you will never get into a situation when your data has been changed by another
thread during successive database reads inside of one query.

You can use simple (not updatable) queries in the main thread with RxQuery to get benefits of database locking and
reactive operators chaining.

An updatable query will run after a data change with some delay. This is done to handle a problem of overproducing observable,
see [debounce](https://github.com/ReactiveX/RxJava/wiki/Backpressure#debounce-or-throttlewithtimeout) RxJava operator.
You can set this delay manually depending on your application needs.

## Example

This is how to register an updatable query which will run every time a change in "messages" table happens:

``` java
q.updatable(new Func0<String>() {
    @Override
    public String call() {
        // your data access methods here
        return "Hello, world!";
    }
}, DataSet.fromTable("messages"))
.subscribe(new Action1<String>() {
    @Override
    public void call(String o) {
        Log.v("", o);
    }
});
```

Output:

``` text
10:00 Hello, world!
```

``` java
q.execution(new Func0<DataDescription>() {
    @Override
    public DataDescription call() {
        // some data change
        ...
        // return a description of what has been changed - a table name and a row id
        return DataSet.fromRow("messages", 12).addRow("users", 11).addRow("groups", 22);
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
will never be changed. Use [AutoParcel](https://github.com/frankiesardo/auto-parcel) library, use [Guava](https://github.com/google/guava)'s
[ImmutableList](https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/ImmutableList.java).
More on benefits of immutable objects you can read in Joshua Bloch's "Effective Java".

Do not expose your database cursor or any other low-level data structure to the view layer of an application.
At first, such objects are not immutable. Second, this breaks layers of logic.
You can read about an effective way to architect your Android application here:
[Architecting Android… The clean way?](http://fernandocejas.com/2014/09/03/architecting-android-the-clean-way/)

The proper way to use this library is inside of your Presenter. (You're already using Model-View-Presenter, isn't it?)
The entire pipeline would look like this (in a pseudocode).

Query some data from a database:

``` java
View:
    getPresenter().querySomeData();

Presenter:
    q.query({ model.readFromDatabase() }).subscribe({ getView().publishSomeData(data); });

Model:
    Data readFromDatabase() {
        your sql / orm / etc calls
        return you data, preferably an AutoParcel object or an ImmutableList of AutoParcel objects.
    }
```

A data change:

``` java
View:
    getPresenter().changeSomeData(data); // data is an AutoParcel object or an ImmutableList of AutoParcel objects.

Presenter:
    q.execute({ model.changeSomeDatabaseRows(data) }).subscribe();

Model:
    DataSet changeSomeDatabaseRows(data)() {
        your sql / orm / etc calls
        return DataSet.fromRow(tableName, changedRowId);
    }
```

An updatable:

``` java
Presenter:
    q.updatable({ model.readFromDatabase() }, DataSet.fromTable(tableName))
        .subscribe({ getView().publishSomeData(data) }); // this part depends on your MVP library: there
                                                         // should be a method to delay onNext until a view become available.
                                                         // See my "nucleus" project for updates soon.
```

## Installation

While this project is in a snapshot version you install it in two steps. At first you need to
include a snapshot repository at the beginning of your `build.gradle`:

``` groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
}
```

Then you can add the library itself:

``` groovy
dependencies {
    compile 'info.android15.rxquery:rxquery:0.1.0-SNAPSHOT'
}
```

## Initialization

``` java
Scheduler backgroundScheduler = Schedulers.from(Executors.newSingleThreadExecutor(new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "database_background");
    }
}));
RxQuery q = new RxQuery(backgroundScheduler, AndroidSchedulers.mainThread(), 150);
```

## Advanced usage

If one day you will need a really fuzzy triggering rules - just create your implementation of `DataPattern` and `DataDescription`
interfaces, and use them instead of `DataSet`. I hardly can imagine a situation when you will need this, but the ability is here.

## Other references

* [SQLite multithreading best practices](http://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android/3689883#3689883)
* [RxJava](https://github.com/ReactiveX/RxJava)
* [Nucleus](https://github.com/konmik/nucleus)
