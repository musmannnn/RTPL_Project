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
package takatuka.offlineGC.OGI.superInstruction;

import java.util.*;
import takatuka.classreader.dataObjs.*;
import takatuka.offlineGC.DFA.dataObjs.TTReference;
import takatuka.offlineGC.DFA.dataObjs.attribute.*;
import takatuka.optimizer.cpGlobalization.logic.util.Oracle;
import takatuka.vm.autoGenerated.forExceptionPrettyPrint.LineNumberController;

/**
 * <p>Title: </p>
 * <p>Description:
 *
 * </p>
 * @author Faisal Aslam
 * @version 1.0
 */
public class SuperInstruction {

    private Vector<GCInstruction> normalInstruction = new Vector<GCInstruction>();
    //private Vector<HashSet<TTReference>> normalInstructionRef = new Vector<HashSet<TTReference>>();
    private HashSet<SuperInstruction> nextInstrSet = new HashSet<SuperInstruction>();
    private MethodInfo method = null;
    private String methodStr = null;
    public boolean visitedOnce = false;
    //only used during computation and later are deleted.
    public HashSet<TTReference> tempRefForComputation = new HashSet<TTReference>();

    public SuperInstruction(MethodInfo method) {
        this.method = method;
        Oracle oracle = Oracle.getInstanceOf();
        methodStr = oracle.getMethodOrFieldString(method);
    }

    @Override
    public String toString() {
        Stack<SuperInstruction> stack = new Stack<SuperInstruction>();
        stack.push(this);
        String ret = "\t";
        HashSet<SuperInstruction> alreadyVisited = new HashSet<SuperInstruction>();
        boolean firstTime = true;
        while (!stack.empty()) {
            SuperInstruction superInstr = stack.pop();
            if (!firstTime) {
                ret += "\n\t";
            }
            ret += superInstr.printMe();
            if (alreadyVisited.contains(superInstr)) {
                ret += "\n--- ALREADY ---\n";
                continue;
            }
            alreadyVisited.add(superInstr);
            stack.addAll(superInstr.nextInstrSet);
            firstTime = false;
        }
        return ret;
    }

    public String printMe() {
        String ret = "";
        ret += "(";//"(/*ref=" + tempRefForComputation.toString() + ": ";
        for (int loop = 0; loop < normalInstruction.size(); loop++) {
            GCInstruction instr = normalInstruction.elementAt(loop);
            int lineNumber = LineNumberController.getInstanceOf().getLineNumberInfo(instr.getInstructionId());

            ret += lineNumber + ":" + instr.getMnemonic() /*+ ":" + normalInstructionRef.elementAt(loop)*/;
            if (loop + 1 < normalInstruction.size()) {
                ret += " + ";
            }
        }
        ret += ")";
        return ret;
    }

    public boolean addNormalInstrs(GCInstruction instr) {
        return normalInstruction.add(instr);
    }

    /**
     *
     * @return
     */
    public Vector<GCInstruction> getNormalInstrs() {
        return normalInstruction;
    }

    /**
     *
     * @param nextInstr
     */
    public void addNextSuperInstr(SuperInstruction nextInstr) {
        nextInstrSet.add(nextInstr);
    }

    /**
     *
     * @return
     */
    public HashSet<SuperInstruction> getNextSuperInstrs() {
        return nextInstrSet;
    }

    /**
     * 
     * @param superInstr
     */
    public void mergeSuperInstruction(SuperInstruction superInstr) {
        normalInstruction.addAll(superInstr.getNormalInstrs());
        nextInstrSet.remove(superInstr);
        nextInstrSet.addAll(superInstr.nextInstrSet);
    }

    /**
     * 
     * @return
     */
    public boolean isBranchInstr() {
        GCInstruction inst = getNormalInstrs().firstElement();
        if (inst.isBranchTarget()
                || inst.isBranchSourceInstruction()
                || inst.getMnemonic().contains("STATIC")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SuperInstruction)) {
            return false;
        }
        SuperInstruction input = (SuperInstruction) obj;
        if (!input.methodStr.equals(methodStr)
                || normalInstruction.size() != input.normalInstruction.size()) {
            return false;
        }
        Iterator<GCInstruction> instrIt = normalInstruction.iterator();
        while (instrIt.hasNext()) {
            GCInstruction instr = instrIt.next();
            Iterator<GCInstruction> inputInstrIt = input.normalInstruction.iterator();
            boolean found = false;
            while (inputInstrIt.hasNext()) {
                GCInstruction inputInstr = inputInstrIt.next();
                if (instr.getInstructionId() == inputInstr.getInstructionId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.normalInstruction != null ? this.normalInstruction.hashCode() : 0);
        hash = 29 * hash + (this.methodStr != null ? this.methodStr.hashCode() : 0);
        return hash;
    }
}
