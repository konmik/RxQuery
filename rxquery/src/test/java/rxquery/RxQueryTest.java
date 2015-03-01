package rxquery;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.TestScheduler;

public class RxQueryTest extends TestCase {

    public static final String QUERY_RESULT = "query_result";

    public void testQuery() throws Exception {
        TestScheduler background = new TestScheduler();
        TestScheduler foreground = new TestScheduler();
        RxQuery q = new RxQuery(background, foreground, 200);
        final AtomicLong queryThread = new AtomicLong();
        final AtomicLong resultThread = new AtomicLong();

        q.query(new Func0<String>() {
            @Override
            public String call() {
                queryThread.set(Thread.currentThread().getId());
                return QUERY_RESULT;
            }
        }).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                assertEquals(QUERY_RESULT, s);
                resultThread.set(Thread.currentThread().getId());
            }
        });

        assertTrue(resultThread.get() != 0);
        assertEquals(queryThread.get(), resultThread.get());
        assertEquals(queryThread.get(), Thread.currentThread().getId());
    }

    public void testUpdatable() throws Exception {
        TestScheduler background = new TestScheduler();
        TestScheduler foreground = new TestScheduler();
        RxQuery q = new RxQuery(background, foreground, 200);

        final AtomicInteger queryCounter = new AtomicInteger();
        final AtomicInteger resultCounter = new AtomicInteger();
        q.updatable(new Func0<String>() {
            @Override
            public String call() {
                queryCounter.incrementAndGet();
                return QUERY_RESULT;
            }
        }, DataSet.fromTable("data")).subscribe(new Action1<String>() {
            @Override
            public void call(String o) {
                assertEquals(QUERY_RESULT, o);
                resultCounter.incrementAndGet();
            }
        });

        assertEquals(1, queryCounter.get());
        assertEquals(1, resultCounter.get());

        q.notifyDataChange(DataSet.fromTable("data"));

        assertEquals(1, queryCounter.get());
        assertEquals(1, resultCounter.get());

        background.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        foreground.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        assertEquals(2, queryCounter.get());
        assertEquals(2, resultCounter.get());
    }

    public void testExecution() throws Exception {
        TestScheduler background = new TestScheduler();
        TestScheduler foreground = new TestScheduler();
        RxQuery q = new RxQuery(background, foreground, 200);

        final AtomicInteger queryCounter = new AtomicInteger();
        final AtomicInteger resultCounter = new AtomicInteger();
        final AtomicInteger executionCounter = new AtomicInteger();

        q.updatable(new Func0<String>() {
            @Override
            public String call() {
                queryCounter.incrementAndGet();
                return QUERY_RESULT;
            }
        }, DataSet.fromTable("data")).subscribe(new Action1<String>() {
            @Override
            public void call(String o) {
                assertEquals(QUERY_RESULT, o);
                resultCounter.incrementAndGet();
            }
        });

        q.execution(new Func0<DataDescription>() {
            @Override
            public DataDescription call() {
                executionCounter.incrementAndGet();
                return DataSet.fromTable("data");
            }
        }).subscribe();

        background.triggerActions();
        foreground.triggerActions();
        assertEquals(1, executionCounter.get());

        background.triggerActions();
        foreground.triggerActions();
        background.triggerActions();
        foreground.triggerActions();
        background.triggerActions();
        foreground.triggerActions();
        background.triggerActions();
        foreground.triggerActions();
        background.triggerActions();
        foreground.triggerActions();

        assertEquals(1, queryCounter.get());
        assertEquals(1, resultCounter.get());

        background.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        foreground.triggerActions();

        assertEquals(2, queryCounter.get());
        assertEquals(2, resultCounter.get());

        q.execution(new Func0<DataDescription>() {
            @Override
            public DataDescription call() {
                executionCounter.incrementAndGet();
                return DataSet.fromTable("da!!!!!!ta");
            }
        }).subscribe();

        background.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        foreground.triggerActions();

        assertEquals(2, queryCounter.get());
        assertEquals(2, resultCounter.get());
    }
}