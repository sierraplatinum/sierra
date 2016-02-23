/*******************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
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
 *******************************************************************************/
package biovis.sierra.client.GUI.GUIHelper;

import java.text.DecimalFormat;
import java.text.ParseException;

import javafx.beans.NamedArg;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.util.StringConverter;
/**
*
* @author Daniel Gerighausen
*/
public class SpinnerValue extends DoubleSpinnerValueFactory {
	
	
	//Adjust these two variable to modify the step size of the spinner
	final double amountToStepBy =  0.000001;
	final String format 		= "#.######";
	
	
    public SpinnerValue(@NamedArg("min") double min,
                                     @NamedArg("max") double max,
                                     @NamedArg("initialValue") double initialValue)
                                    {
        super(min,max,initialValue);
    	setMin(min);
        setMax(max);
        setAmountToStepBy(amountToStepBy);
        setConverter(new StringConverter<Double>() {
            private final DecimalFormat df = new DecimalFormat(format);

            @Override public String toString(Double value) {
                // If the specified value is null, return a zero-length String
                if (value == null) {
                    return "";
                }

                return df.format(value);
            }

            @Override public Double fromString(String value) {
                try {
                    // If the specified value is null or zero-length, return null
                    if (value == null) {
                        return null;
                    }

                    value = value.trim();

                    if (value.length() < 1) {
                        return null;
                    }

                    // Perform the requested parsing
                    return df.parse(value).doubleValue();
                } catch (ParseException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        valueProperty().addListener((o, oldValue, newValue) -> {
            // when the value is set, we need to react to ensure it is a
            // valid value (and if not, blow up appropriately)
            if (newValue < getMin()) {
                setValue(getMin());
            } else if (newValue > getMax()) {
                setValue(getMax());
            }
        });
        setValue(initialValue >= min && initialValue <= max ? initialValue : min);
    }
	

	


}
