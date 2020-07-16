package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
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

import org.bndly.schema.beans.ActiveRecord;

public class PersistableActiveRecord {
    private final ActiveRecord activeRecord;
    private boolean persistenceScheduled;

    public PersistableActiveRecord(ActiveRecord activeRecord) {
        this.activeRecord = activeRecord;
    }

    public boolean isPersisted() {
        return activeRecord.getId() != null;
    }

    public ActiveRecord getActiveRecord() {
        return activeRecord;
    }
    
    public boolean isPersistenceScheduled() {
        return persistenceScheduled;
    }

    public void setPersistenceScheduled(boolean persistenceScheduled) {
        this.persistenceScheduled = persistenceScheduled;
    }
    
}
