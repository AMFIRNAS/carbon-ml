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
package org.wso2.carbon.ml.core.factories;

/**
 * Holds all dataset types.
 */
public enum DatasetType {
    FILE("file"), DAS("das"), HDFS("hdfs"), TIME_SERIES("time_series");

    private String value = null;

    private DatasetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    /**
     * get {@link DatasetType} for a given value.
     */
    public static DatasetType getDatasetType(String value) {
        for (DatasetType type : DatasetType.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
