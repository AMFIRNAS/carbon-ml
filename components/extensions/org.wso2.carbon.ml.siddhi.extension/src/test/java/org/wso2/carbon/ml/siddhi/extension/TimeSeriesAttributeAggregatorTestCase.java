package org.wso2.carbon.ml.siddhi.extension;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;

/**
 * Created by amfirnas on 6/28/16.
 */

public class TimeSeriesAttributeAggregatorTestCase {

    private static Logger logger = Logger.getLogger(TimeSeriesAttributeAggregatorTestCase.class);
    protected static SiddhiManager siddhiManager;

    @Test
    public void testProcess() throws Exception {
        logger.info("TimeSeriesAttributeAggregatorExtension TestCase");

        siddhiManager = new SiddhiManager();
        String inValueStream = "define stream InValueStream (inValue float);";

        String eventFuseExecutionPlan =   ("@info(name = 'query1') from InValueStream "
                + "select timeseries:slide(inValue,5) as timeslides "
                + "insert into OutMediationStream;");
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inValueStream + eventFuseExecutionPlan);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents,
                                Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                Long result;
                for (Event event : inEvents) {
                    result = (Long) event.getData(0);
                    Assert.assertEquals((Long) 6l, result);
                }
            }
        });
        InputHandler inputHandler = executionPlanRuntime
                .getInputHandler("InValueStream");
        executionPlanRuntime.start();
//        inputHandler.send(new Float[]{23.0d,24.0d,25.0d});
//        inputHandler.send(new Float[]{26.0f,27.0f,28.0f});
//        inputHandler.send(new Float[]{29.0f,30.0f,31.0f});
        // inputHandler.send(new Object[]{2.0f});
        inputHandler.send(new Float[]{27.0f});
        inputHandler.send(new Float[]{28.0f});
        inputHandler.send(new Float[]{29.0f});
        inputHandler.send(new Float[]{30.0f});
        inputHandler.send(new Float[]{31.0f});
        inputHandler.send(new Float[]{32.0f});
//        inputHandler.send(new Double[]{30.0});
//        inputHandler.send(new Double[]{31.0});
//        inputHandler.send(new Double[]{32.0});

//
//        Random randomGenerator = new Random();
//
//        for(int i=0 ; i<10 ;i++){
//
//            inputHandler.send(new Integer[]{randomGenerator.nextInt()});
//
//        }
//
//



        Thread.sleep(100);
        executionPlanRuntime.shutdown();
    }
}

