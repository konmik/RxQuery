
# RxQuery

RxQuery is a very simple (stupid simple) library that adds RxJava functionality to your database layer.

This library is not limited by any ORM or direct SQL access. You can use ANY database type you wish.

The core of this library is updatable query which returns an object of query-specific result type.
You can trigger your updatable queries by a table change or by a change in a specific data row,
or by any set of tables and rows, or you can even use your own triggering rules.

Data modification and query updates happen in a background thread, with automatic delivery
to the foreground thread. RxQuery creates a lock for every query, so
you will never get into a situation when your data has been changed by another
thread during successive database reads inside of one query.

You can use simple queries in the main thread with RxQuery to get benefits of database locking
and reactive operators chaining.

Updatable query will run after a data change with some delay. This is done to handle a problem of overproducing observable,
see [debounce](https://github.com/ReactiveX/RxJava/wiki/Backpressure#debounce-or-throttlewithtimeout) RxJava operator.
You can set this delay manually depending on your application needs.

## Example

Registering an updatable query which will run every time a change in "messages" table happens:

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
Hello, world!
```

``` java
q.execution(new Func0<DataDescription>() {
    @Override
    public DataDescription call() {
        // some data change
        ...
        // return a description of what has been changed - a table name and a row id
        return DataSet.fromRow("messages", 12).addRow("messages", 11);
    }
}).subscribe();
```

Output:

``` text
Hello, world!
Hello, world!
```

## Installation

``` groovy
compile 'info.android15.rxquery:rxquery:0.1.0'
```

## Initialization

``` java
Scheduler backgroundScheduler = Schedulers.from(Executors.newSingleThreadExecutor(new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "database_thread");
    }
}));
RxQuery q = new RxQuery(backgroundScheduler, AndroidSchedulers.mainThread(), 150);
```

## Advanced usage

If one day you will need a really fuzzy triggering rules - just create your implementation of `DataPattern` and `DataDescription`
interfaces, and use them instead of `DataSet`.

