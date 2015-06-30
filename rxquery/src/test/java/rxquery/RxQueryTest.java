package rxquery;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.TestScheduler;

public class RxQueryTest extends TestCase {

    public static final String QUERY_RESULT = "query_result";

    public void testImmediate() throws Exception {
        TestScheduler background = new TestScheduler();
        TestScheduler foreground = new TestScheduler();
        Action1<Action0> executor = new Action1<Action0>() {
            @Override
            public void call(Action0 action0) {
                action0.call();
            }
        };
        RxQuery q = new RxQuery(background, foreground, 200, executor);
        final AtomicLong queryThread = new AtomicLong();
        final AtomicLong resultThread = new AtomicLong();

        q.immediate(new Func0<String>() {
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
        Action1<Action0> executor = new Action1<Action0>() {
            @Override
            public void call(Action0 action0) {
                action0.call();
            }
        };
        RxQuery q = new RxQuery(background, foreground, 200, executor);

        final AtomicInteger queryCounter = new AtomicInteger();
        Subscription subscription1 = q.updatable("data", new Func0<String>() {
            @Override
            public String call() {
                queryCounter.incrementAndGet();
                return QUERY_RESULT;
            }
        }).subscribe();

        foreground.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        assertEquals(1, queryCounter.get());

        final Action0 action0 = new Action0() {
            @Override
            public void call() {

            }
        };
        Subscription subscription2 = q.execution("data", new Action0() {
            @Override
            public void call() {
                action0.call();
            }
        }).subscribe();

        background.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        foreground.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        assertEquals(2, queryCounter.get());
    }

    public void testExecution() throws Exception {
        TestScheduler background = new TestScheduler();
        TestScheduler foreground = new TestScheduler();
        Action1<Action0> executor = new Action1<Action0>() {
            @Override
            public void call(Action0 action0) {
                action0.call();
            }
        };
        RxQuery q = new RxQuery(background, foreground, 200, executor);

        final AtomicInteger queryCounter = new AtomicInteger();
        final AtomicInteger executionCounter = new AtomicInteger();

        q.updatable("data", new Func0<String>() {
            @Override
            public String call() {
                queryCounter.incrementAndGet();
                return QUERY_RESULT;
            }
        }).subscribe();

        q.execution("data", new Action0() {
            @Override
            public void call() {
                executionCounter.incrementAndGet();
            }
        }).subscribe();

        background.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        assertEquals(1, executionCounter.get());
        assertEquals(2, queryCounter.get());

        q.execution("da!!!!!!ta", new Action0() {
            @Override
            public void call() {
                executionCounter.incrementAndGet();
            }
        }).subscribe();

        background.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        assertEquals(2, queryCounter.get());
    }
}