/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat;

public class HeatPointSystem {
    public static final int MIN = 0;
    public static final int MAX = 10;

    public final int POINTS;

    public HeatPointSystem(int heatValue) {
        if (heatValue < MIN || heatValue > MAX) {
            String err = String.format("Only values between %d and %d allowed. Given: %d", MIN, MAX,
                    heatValue);
            throw new IllegalArgumentException(err);
        }

        this.POINTS = heatValue;
    }

    public int diff(HeatPointSystem b) {
        return POINTS - b.POINTS;
    }

    public boolean isGreaterThan(HeatPointSystem b) {
        return POINTS > b.POINTS;
    }

    @Override
    public String toString() {
        return "HeatPointSystem{" + POINTS + '}';
    }
}
