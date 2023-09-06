package com.navigatingcancer.healthtracker.api.events;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private Executor exec = null;

    private synchronized Executor internalGetAsyncExecutor() {
        if (exec == null) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            // TODO. Can set some thread pool options. Like pool and queue size.
            executor.setThreadNamePrefix("AsyncExecutor-");
            executor.initialize();
            exec = executor;
        }
        return exec;
    }

    // Note. Need this mostly for the testing purpose.
    // Need some way to explicitly flush the executor queues
    @Override
    public Executor getAsyncExecutor() {
        if( exec == null ) {
            exec = internalGetAsyncExecutor();
        }
        return exec;
    }

    // TODO. It may be a good idea to handle async exceptions
    // @Override
    // public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    //    return new MyAsyncUncaughtExceptionHandler();
    // }    

}

