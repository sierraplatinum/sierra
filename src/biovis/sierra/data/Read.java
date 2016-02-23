/**
 *****************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
 *
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
 ******************************************************************************
 */
package biovis.sierra.data;

import htsjdk.samtools.SAMRecord;

/**
 *
 * @author Dirk Zeckzer
 */
public class Read {

    int start = 0;
    int end = 0;
    boolean unmappedFlag = false;
    byte[] baseQualities;
    String baseQualitiesASCII;

    /**
     * Constructor.
     * @param record
     */
    public Read(
            final SAMRecord record
    ) {
        start = record.getStart();
        end = record.getEnd();
        unmappedFlag = record.getReadUnmappedFlag();
        baseQualities = record.getBaseQualities();
        baseQualitiesASCII = record.getBaseQualityString();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean getReadUnmappedFlag() {
        return unmappedFlag;
    }

    public byte[] getBaseQualities() {
        return baseQualities;
    }

    public String getBaseQualitiesASCII() {
        return baseQualitiesASCII;
    }
}
