/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.e2e;

import java.util.UUID;

import org.jdbi.v3.core.statement.JdbiStatementEvent;
import org.jdbi.v3.core.statement.internal.JfrSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JfrNativeTest {
    @Test
    public void jdbiSpinup() {
        assertThat(JfrSupport.isJfrAvailable()).withFailMessage("JFR is not available on this platform").isTrue();
        assertThat(JfrSupport.isFlightRecorderAvailable()).withFailMessage("Flight Recorder is not available").isTrue();
    }

    @Test
    public void jdbiEvent() throws Exception {
        var event = JfrSupport.newStatementEvent();
        event.begin();
        Thread.sleep(3000L);

        assertThat(event).withFailMessage("JFR was not activated!").isInstanceOf(JdbiStatementEvent.class);
        if (event.shouldCommit()) {
            var jdbiEvent = (JdbiStatementEvent) event;
            jdbiEvent.traceId = UUID.randomUUID().toString();
            jdbiEvent.type = "Update";
            jdbiEvent.sql = "THE SQL!";
            jdbiEvent.commit();
        }
    }
}
