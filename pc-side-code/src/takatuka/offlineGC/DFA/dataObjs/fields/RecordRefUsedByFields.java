/*
 * Copyright 2010 Christian Schindelhauer, Peter Thiemann, Faisal Aslam, Luminous Fennell and Gidon Ernst.
 * All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 3 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Faisal Aslam 
 * (aslam AT informatik.uni-freibug.de or studentresearcher AT gmail.com)
 * if you need additional information or have any questions.
 */
package takatuka.offlineGC.DFA.dataObjs.fields;

import java.util.*;
import takatuka.offlineGC.DFA.dataObjs.*;
import takatuka.offlineGC.DFA.dataObjs.attribute.GCInstruction;
import takatuka.offlineGC.DFA.logic.factory.NewInstrIdFactory;
import takatuka.optimizer.cpGlobalization.logic.util.Oracle;
import takatuka.vm.autoGenerated.forExceptionPrettyPrint.LineNumberController;

/**
 * 
 * Description:
 * <p>
 * Keep record of all the references that are get from a  field of any kind.
 * This information is useful when deciding freeing references created in a method.
 *
 * </p> 
 * @author Faisal Aslam
 * @version 1.0
 */
public class RecordRefUsedByFields {

    private static final RecordRefUsedByFields myObj = new RecordRefUsedByFields();
    private HashSet<TTReference> refsAssignedToAField = new HashSet<TTReference>();

    /**
     * the constructor is private
     */
    private RecordRefUsedByFields() {
    }

    /**
     * 
     * @return
     * the instance of current class
     */
    public static RecordRefUsedByFields getInstanceOf() {
        return myObj;
    }

    /**
     * record when a reference is get from a field.
     * @param value
     */
    public void recordRefIsGetFromAField(GCField value) {
        if (value == null) {
            return;
        }
        refsAssignedToAField.addAll(value.get());
    }

    /**
     * 
     * @return
     */
    public HashSet<TTReference> getAllSavedRecord() {
        return (HashSet<TTReference>) refsAssignedToAField.clone();
    }

    public boolean containsNewId(int newId) {
        HashSet<TTReference> refSet = getAllSavedRecord();
        Iterator<TTReference> it = refSet.iterator();
        while (it.hasNext()) {
            TTReference ref = it.next();
            if (ref.getNewId() == newId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        Iterator<TTReference> refIt = refsAssignedToAField.iterator();
        String ret = "";
        Oracle oracle = Oracle.getInstanceOf();
        LineNumberController lineNumberContr = LineNumberController.getInstanceOf();
        while (refIt.hasNext()) {
            TTReference ref = refIt.next();
            int newId = ref.getNewId();
            if (newId > 0) {
                GCInstruction instr = (GCInstruction) NewInstrIdFactory.getInstanceOf().getInstrANewIdAssignedTo(newId);
                int linNumber = LineNumberController.getInstanceOf().getLineNumberInfo(instr);
                String methodString = oracle.getMethodOrFieldString(instr.getMethod());
                ret += "new Id at line Number =" + linNumber + " of method =" + methodString + "\n";
            }
        }
        return ret;
    }
}
