/*-
 * #%L
 * HAPI FHIR JPA Model
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.model.sched;

import org.quartz.JobKey;
import org.quartz.SchedulerException;

import java.util.Set;

public interface IHapiScheduler {
	void init() throws SchedulerException;

	void start();

	void shutdown();

	boolean isStarted();

	void clear() throws SchedulerException;

	void logStatusForUnitTest();

	/**
	 * Pauses this scheduler (and thus all scheduled jobs).
	 * To restart call {@link #unpause()}
	 */
	void pause();

	/**
	 * Restarts this scheduler after {@link #pause()}
	 */
	void unpause();

	void scheduleJob(long theIntervalMillis, ScheduledJobDefinition theJobDefinition);

	Set<JobKey> getJobKeysForUnitTest() throws SchedulerException;

	default void triggerJobImmediately(ScheduledJobDefinition theJobDefinition) {}
}
