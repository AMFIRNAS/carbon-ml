/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.ml.core.impl;

import org.wso2.carbon.ml.commons.constants.MLConstants;
import org.wso2.carbon.ml.commons.domain.MLDataset;
import org.wso2.carbon.ml.commons.domain.SamplePoints;
import org.wso2.carbon.ml.core.exceptions.MLDataProcessingException;
import org.wso2.carbon.ml.core.exceptions.MLInputValidationException;
import org.wso2.carbon.ml.core.exceptions.MLMalformedDatasetException;
import org.wso2.carbon.ml.core.exceptions.MLOutputAdapterException;
import org.wso2.carbon.ml.core.factories.DatasetType;
import org.wso2.carbon.ml.core.interfaces.DatasetProcessor;
import org.wso2.carbon.ml.core.interfaces.MLOutputAdapter;
import org.wso2.carbon.ml.core.utils.MLCoreServiceValueHolder;
import org.wso2.carbon.ml.core.utils.MLUtils;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.io.*;
import java.util.Scanner;


/**
 * Created by amfirnas on 6/22/16.
 */

/**
 * this class is for handling time series dataset
 */

public class TimeSeriesDatasetProcessor extends DatasetProcessor  {

    private InputStream inputStream = null;
    private int windowLength;

    public TimeSeriesDatasetProcessor(MLDataset dataset, InputStream inputStream) throws MLInputValidationException {
        super(DatasetType.TIME_SERIES, dataset);
        this.inputStream = inputStream;
        this.validate();
    }

    @Override
    public void validate() throws MLInputValidationException {
        super.validate();
        try {
            if (inputStream == null || inputStream.available() == 0) {
                String msg = "Input stream is null or empty for dataset: " + getDataset().getName();
                handleValidationException(msg);
            }
        } catch (IOException e) {
            String msg = "Invalid input stream for datasett: " + getDataset().getName();
            handleValidationException(msg, e);
        }
    }

    @Override
    public void process() throws MLDataProcessingException {
        try {
            MLDataset dataset = getDataset();
            MLCoreServiceValueHolder valueHolder = MLCoreServiceValueHolder.getInstance();
            MLIOFactory ioFactory = new MLIOFactory(valueHolder.getMlProperties());
            MLOutputAdapter outputAdapter = ioFactory.getOutputAdapter(dataset.getDataTargetType()
                    + MLConstants.OUT_SUFFIX);
            setTargetPath(ioFactory.getTargetPath(dataset.getName() + "." + dataset.getTenantId() + "."
                    + System.currentTimeMillis()));

            //window size getting from user
            int windowLength = dataset.getWindowLength();

            //convert user uploading dataset to slided window format
            convertToTimeseriesDataFormat(inputStream,windowLength);

            //transformed dataset path in server
            String path =  System.getProperty("user.dir")+"/datasets/file.csv";

            //read the input stream
            InputStream is = new FileInputStream(path);

            outputAdapter.write(getTargetPath(),is);

            setFirstLine(MLUtils.getFirstLine(getTargetPath()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MLOutputAdapterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    handleIgnoreException("Failed to close the input stream.", e);
                }
            }
        }
    }


    @Override
    public SamplePoints takeSample() throws MLDataProcessingException {
        MLDataset dataset = getDataset();
        MLCoreServiceValueHolder valueHolder = MLCoreServiceValueHolder.getInstance();
        try {
            return MLUtils.getSample(getTargetPath(), dataset.getDataType(), valueHolder.getSummaryStatSettings()
                    .getSampleSize(), dataset.isContainsHeader());
        } catch (MLMalformedDatasetException e) {
            throw new MLDataProcessingException(e.getMessage(), e);
        }
    }


    /**
     *  convert user uploading dataset format to slided window dataset format by using siddhi inside
     *
     *
     * @param inputStream
     * @param windowLength
     * @throws InterruptedException
     * @throws IOException
     */

    private void convertToTimeseriesDataFormat(InputStream inputStream,int windowLength) throws InterruptedException, IOException {

        DataInputStream dis = null;
        DataOutputStream dos = null;
        SiddhiManager siddhiManager;

        siddhiManager = new SiddhiManager();
        String inValueStream = "define stream InValueStream (inValue float);";

        //use windowLength value
        String eventFuseExecutionPlan = ("@info(name = 'query1') from InValueStream "
                + "select timeseries:slide(inValue, "+windowLength+") as timeslides "
                + "insert into OutMediationStream;");
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inValueStream + eventFuseExecutionPlan);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents,
                                Event[] removeEvents) {
               // EventPrinter.print(timeStamp, inEvents, removeEvents);
//                Long result;
//                for (Event event : inEvents) {
//                    result = (Long) event.getData(0);
//                }
            }
        });


        InputHandler inputHandler = executionPlanRuntime
                .getInputHandler("InValueStream");

        executionPlanRuntime.start();

        try{
            Scanner s = new Scanner(inputStream).useDelimiter("\n");
            while(s.hasNext()) {
                String val = s.next();
                try {
                    float floatVal = Float.parseFloat(val);
                    inputHandler.send(new Float[]{floatVal});
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

            s.close();
        }catch(Exception e){
            // if any I/O error occurs
            e.printStackTrace();
        }finally{

            // releases all system resources from the streams
            if(inputStream!=null)
                inputStream.close();
            if(dos!=null)
                inputStream.close();
            if(dis!=null)
                dis.close();
        }



        Thread.sleep(100);
        executionPlanRuntime.shutdown();



    }

}
