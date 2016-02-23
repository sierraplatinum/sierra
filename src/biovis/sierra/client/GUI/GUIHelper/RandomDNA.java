/** *****************************************************************************
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
 ****************************************************************************** */
package biovis.sierra.client.GUI.GUIHelper;

import java.util.Random;

/**
 *
 * @author Daniel Gerighausen
 */
public class RandomDNA {

    public String generateStrand() {
        StringBuilder strand = new StringBuilder();
        Random randomGenerator = new Random();

        for (int i = 0; i < 25; i++) {
            int randomInt = randomGenerator.nextInt(100);

            int nucleotide = randomInt % 4;

            switch (nucleotide) {
                case 0:
                    strand.append('A');
                    break;
                case 1:
                    strand.append('T');
                    ;
                    break;
                case 2:
                    strand.append('C');
                    ;
                    break;
                case 3:
                    strand.append('G');
                    ;
                    break;
            }

        }

        return strand.toString();

    }

    public String getNucleotide(String strandString) {
        Random randomGenerator = new Random();

        StringBuilder strand = new StringBuilder(strandString);
        int randomInt = randomGenerator.nextInt(100);

        int nucleotide = randomInt % 4;

        switch (nucleotide) {
            case 0:
                strand.append('A');
                break;
            case 1:
                strand.append('T');
                ;
                break;
            case 2:
                strand.append('C');
                ;
                break;
            case 3:
                strand.append('G');
                ;
                break;
        }

        strand.deleteCharAt(0);

        return strand.toString();
    }

}
