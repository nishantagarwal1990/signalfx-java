/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Includes work from:
/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.signalfx;

import io.micrometer.core.instrument.AbstractDistributionSummary;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.TimeWindowMax;
import io.micrometer.core.instrument.step.StepTuple2;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * This class is mostly the same as
 * {@link io.micrometer.core.instrument.step.StepDistributionSummary}, with one notable
 * difference: the {@link DistributionStatisticConfig} is modified before being passed to
 * the super class constructor - that forces the histogram generated by this meter to be
 * cumulative.
 *
 * @author Bogdan Drutu
 * @author Mateusz Rzeszutek
 */
final class SignalfxDistributionSummary extends AbstractDistributionSummary {

    private final LongAdder count = new LongAdder();

    private final DoubleAdder total = new DoubleAdder();

    private final StepTuple2<Long, Double> countTotal;

    private final TimeWindowMax max;

    SignalfxDistributionSummary(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig,
            double scale, long stepMillis) {
        super(id, clock, CumulativeHistogramConfigUtil.updateConfig(distributionStatisticConfig), scale, false);
        this.countTotal = new StepTuple2<>(clock, stepMillis, 0L, 0.0, count::sumThenReset, total::sumThenReset);
        max = new TimeWindowMax(clock, distributionStatisticConfig);
    }

    @Override
    protected void recordNonNegative(double amount) {
        count.increment();
        total.add(amount);
        max.record(amount);
    }

    @Override
    public long count() {
        return countTotal.poll1();
    }

    @Override
    public double totalAmount() {
        return countTotal.poll2();
    }

    @Override
    public double max() {
        return max.poll();
    }

}
