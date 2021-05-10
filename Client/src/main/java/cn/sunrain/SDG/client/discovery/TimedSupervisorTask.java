package cn.sunrain.SDG.client.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lin
 * @date 2021/3/4 15:41
 */
public class TimedSupervisorTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(TimedSupervisorTask.class);


    private final ScheduledExecutorService scheduler;
    private final ThreadPoolExecutor executor;
    private final long timeoutMillis;
    private final Runnable task;

    private final AtomicLong delay;
    private final long maxDelay;

    public TimedSupervisorTask(ScheduledExecutorService scheduler, ThreadPoolExecutor executor,
                               int timeout, TimeUnit timeUnit, int expBackOffBound, Runnable task) {
        this.scheduler = scheduler;
        this.executor = executor;
        this.timeoutMillis = timeUnit.toMillis(timeout);
        this.task = task;
        this.delay = new AtomicLong(timeoutMillis);
        this.maxDelay = timeoutMillis * expBackOffBound;

    }

    @Override
    public void run() {
        Future<?> future = null;
        try {
            //使用Future，可以设定子线程的超时时间，这样当前线程就不用无限等待了
            future = executor.submit(task);
            //指定等待子线程的最长时间
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);  // block until done or timeout
            //delay是个很有用的变量，后面会用到，这里记得每次执行任务成功都会将delay重置
            delay.set(timeoutMillis);
        } catch (TimeoutException e) {
            logger.error("task supervisor timed out", e);

            long currentDelay = delay.get();
            //任务线程超时的时候，就把delay变量翻倍，但不会超过外部调用时设定的最大延时时间
            long newDelay = Math.min(maxDelay, currentDelay * 2);
            //设置为最新的值，考虑到多线程，所以用了CAS
            delay.compareAndSet(currentDelay, newDelay);
        } catch (RejectedExecutionException e) {
            //一旦线程池的阻塞队列中放满了待处理任务，触发了拒绝策略，就会将调度器停掉
            if (executor.isShutdown() || scheduler.isShutdown()) {
                logger.warn("task supervisor shutting down, reject the task", e);
            } else {
                logger.error("task supervisor rejected the task", e);
            }

        } catch (Throwable e) {
            //一旦出现未知的异常，就停掉调度器
            if (executor.isShutdown() || scheduler.isShutdown()) {
                logger.warn("task supervisor shutting down, can't accept the task");
            } else {
                logger.error("task supervisor threw an exception", e);
            }

        } finally {
            //这里任务要么执行完毕，要么发生异常，都用cancel方法来清理任务；
            if (future != null) {
                future.cancel(true);
            }

            //只要调度器没有停止，就再指定等待时间之后在执行一次同样的任务
            if (!scheduler.isShutdown()) {
                //这里就是周期性任务的原因：只要没有停止调度器，就再创建一次性任务，执行时间时dealy的值，
                //假设外部调用时传入的超时时间为30秒（构造方法的入参timeout），最大间隔时间为50秒(构造方法的入参expBackOffBound)
                //如果最近一次任务没有超时，那么就在30秒后开始新任务，
                //如果最近一次任务超时了，那么就在50秒后开始新任务（异常处理中有个乘以二的操作，乘以二后的60秒超过了最大间隔50秒）
                scheduler.schedule(this, delay.get(), TimeUnit.MILLISECONDS);
            }
        }
    }
}

