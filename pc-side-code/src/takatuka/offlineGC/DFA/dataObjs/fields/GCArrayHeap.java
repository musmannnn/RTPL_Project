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
import takatuka.classreader.dataObjs.*;
import takatuka.classreader.logic.util.Miscellaneous;
import takatuka.offlineGC.DFA.dataObjs.*;
import takatuka.offlineGC.DFA.dataObjs.attribute.*;

/**
 * 
 * Description:
 * <p>
 * </p> 
 * @author Faisal Aslam
 * @version 1.0
 */
public class GCArrayHeap extends GCHeap {

    /**
     * ignoring the indexes as they are not always available.
     */
    private GCField field = new GCField(-1);
    
    public GCArrayHeap(int newInstrId, int classId) {
        super(newInstrId, classId);
    }

    @Override
    public int getNewInstrId() {
        return super.getNewInstrId();
    }

    @Override
    public GCField getField(int index, MethodInfo method, Vector callingParams,
            GCInstruction instr) {
        gpRec.recordRefIsGetFromAField(field);
        return field;
    }

    @Override
    public void putField(int index, GCType value, MethodInfo method, 
            Vector callingParams, GCInstruction instr) throws Exception {
        if (!value.isReference()) {
            Miscellaneous.printlnErr("Error # 2344");
            System.exit(1);
        }
        field.add(value.getReferences());
        //gpRec.record(field);
    }
}
