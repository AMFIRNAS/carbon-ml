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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Created by amfirnas on 6/22/16.
 */

/**
 * this class is for handling time series dataset
 */

public class TimeSeriesDatasetProcessor extends DatasetProcessor {

    private InputStream inputStream = null;

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
            convertToTimeseriesDataFormat(inputStream, windowLength);

            //transformed dataset path in server
            String path = System.getProperty("user.dir") + "/datasets/file.csv";

            //read the input stream
            InputStream is = new FileInputStream(path);

            outputAdapter.write(getTargetPath(), is);

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
     * convert user uploading dataset format to slided window dataset format by using siddhi inside
     *
     * @param inputStream
     * @param windowLength
     * @throws InterruptedException
     * @throws IOException
     */

    private void convertToTimeseriesDataFormat(InputStream inputStream, int windowLength) throws InterruptedException, IOException {

        SiddhiManager siddhiManager;
        siddhiManager = new SiddhiManager();
        MLDataset dataset = getDataset();


        Scanner s1 = new Scanner(inputStream).useDelimiter("\n");

        String[] inputValues1 = null;
        String features = "";
        String selectFeatures = "";

        boolean checked1 = true;

        String featureSelects[] = (dataset.getFeatureSelectName()).split(",");
        String stringFeatures[] = (dataset.getStringFeatures()).split(",");
        int[] trackIndex = new int[featureSelects.length];
        List<Integer> trackIndex1 = new ArrayList<>();
        List<Integer> stringIndex1 = new ArrayList<>();

        int[] stringIndex = new int[stringFeatures.length];
        int h = 0;
        int s = 0;
        while (s1.hasNext()) {

            String val = s1.next();
            inputValues1 = val.split(",");
            for (int i = 2; i < inputValues1.length; i++) {
                for (String a : featureSelects) {

                    // System.out.println(a);
                    // System.out.println(Arrays.toString(featureSelects));
                    if (a.equalsIgnoreCase(inputValues1[i])) {
                        trackIndex1.add(i);
                    }

                }

                for (String b : stringFeatures) {

                    //System.out.println(b);
                    //System.out.println(Arrays.toString(stringFeatures));
                    if (b.equalsIgnoreCase(inputValues1[i])) {
                        stringIndex1.add(i);

                    }

                }

                System.out.println(stringIndex1);
                System.out.println(trackIndex1);
                System.out.println(stringFeatures);

                if (!(stringIndex1.contains(i)) && !(trackIndex1.contains(i))) {
                    if (i != inputValues1.length - 1)
                        features += inputValues1[i] + " float ,";
                    if (i == inputValues1.length - 1)
                        features += inputValues1[i] + " float ";
                } else if (stringIndex1.contains(i)) {
                    if (i != inputValues1.length - 1)
                        features += inputValues1[i] + " string ,";
                    if (i == inputValues1.length - 1)
                        features += inputValues1[i] + " string ";

                } else if (trackIndex1.contains(i)) {
                    if (i != inputValues1.length - 1)
                        features += inputValues1[i] + " double ,";
                    if (i == inputValues1.length - 1)
                        features += inputValues1[i] + " double ";

                }

                selectFeatures += inputValues1[i] + ",";

            }
            break;

        }


        String inValueStream = "define stream InValueStream (" + features + ");";

        //use windowLength value
        String eventFuseExecutionPlan = ("@info(name = 'query') from InValueStream "
                + "select timeseries:slide(" + selectFeatures + "" + windowLength + ") as timeslides "
                + "insert into OutMediationStream;");
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inValueStream + eventFuseExecutionPlan);

        executionPlanRuntime.addCallback("query", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents,
                                Event[] removeEvents) {
            }
        });


        InputHandler inputHandler = executionPlanRuntime
                .getInputHandler("InValueStream");

        executionPlanRuntime.start();

        try {
            // Scanner s = new Scanner(inputStream).useDelimiter("\n");
            String[] features1 = selectFeatures.split(",");
            Object[] event = null;
            while (s1.hasNext()) {

                String val = s1.next();
                String[] inputValues = val.split(",");

                int noOfFeatures = features1.length;

                event = new Object[noOfFeatures];
                int m = 0;

                for (int i = 2; i < inputValues.length; i++) {


                    if (!(stringIndex1.contains(i)) && !(trackIndex1.contains(i))) {
                        event[m] = Float.parseFloat(inputValues[i]);
                        m++;
                    } else if (stringIndex1.contains(i)) {
                        event[m] = ((String) inputValues[i]);
                        m++;
                    } else if (trackIndex1.contains(i)) {
                        event[m] = Double.parseDouble(inputValues[i]);
                        m++;
                    }

                }


                try {
                    inputHandler.send(event);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {

            if (inputStream != null)
                inputStream.close();
        }
        Thread.sleep(100);


    }

}
