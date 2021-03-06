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
package takatuka.vm.autoGenerated.vmSwitch;

import java.util.*;
import takatuka.classreader.dataObjs.*;
import takatuka.classreader.dataObjs.attribute.*;
import takatuka.classreader.logic.*;
import takatuka.optimizer.cpGlobalization.logic.util.*;
import takatuka.classreader.logic.util.*;

/**
 * 
 * Description:
 * <p>
 *
 * </p> 
 * @author Faisal Aslam
 * @version 1.0
 */
public class RegisterCustomInstruction {

    private final static RegisterCustomInstruction regCustomInst = new RegisterCustomInstruction();
    private boolean recieveMultiInst = false;
    private int totalNumberOfMultInstr = 0;
    private long totalLengthOfMultInstr = 0;
    private int maxSizeOfMultInstr = 0;

    private RegisterCustomInstruction() {
        startOrEndOfAnOptimization();
    //no one create me but me
    }
    HashMap<Integer, MultiInstrOptimRecord> newInstrToOldInstsMap = new HashMap();
    HashMap<Integer, SingleInstrOptimRecord> newInstrToSingleOldInstMap = new HashMap();
    //private HashMap<Instruction, Vector> NewToOldInstructionsMap = new HashMap();
    private TreeSet cahcedNonUsedOpcodes = null;
    private HashMap<String, Integer> registerOpcodes = new HashMap();
    private boolean dirty = true;
    private boolean newRegistration = false;
    private Oracle oracle = Oracle.getInstanceOf();

    public static RegisterCustomInstruction getInstanceOf() {
        return regCustomInst;
    }

    public void putWatchOnNewRegistration() {
        newRegistration = false;
    }

    public boolean newRegistrationArrived() {
        return newRegistration;
    }

    public void startOrEndOfAnOptimization() {
        if (!dirty) {
            return;
        }
        if (StartMeAbstract.getCurrentState() == StartMeAbstract.STATE_READ_FILES) {
            dirty = false;
        }
        HashSet<OpcodeMnemonicInUse> opMnInUse = oracle.getAllOpcodeMnemonicsUseInCode();
        TreeSet<Integer> allPossibleOpcode = BytecodeProcessor.getAllPossibleOpCodes();
        //Miscellaneous.println("------- here here 1="+allPossibleOpcode.size());
        Iterator<OpcodeMnemonicInUse> itInUse = opMnInUse.iterator();
        while (itInUse.hasNext()) {
            int opcodeInUse = itInUse.next().opCode;
            allPossibleOpcode.remove(new Integer(opcodeInUse));
        }
        cahcedNonUsedOpcodes = allPossibleOpcode;
    }
    /*public Vector<Instruction> getOldInstructions(Instruction newInstr) {
    return NewToOldInstructionsMap.get(newInstr);
    }*/

    public int numberOfOpCodesavailable() {
        if (cahcedNonUsedOpcodes == null) {
            startOrEndOfAnOptimization();
        }
        return cahcedNonUsedOpcodes.size();
    }

    //returns true if instruction is newly saved
    private boolean saveAndGetOpcode(Instruction newInst,
            Vector<Instruction> oldInstructions) {
        Integer opcode = registerOpcodes.get(newInst.getMnemonic());
        if (opcode != null) {
            newInst.setOpCode(opcode);
            return false; //already saved
        }
        Integer ret = (Integer) cahcedNonUsedOpcodes.first();
        cahcedNonUsedOpcodes.remove(ret);
        newInst.setOpCode(ret);
        registerOpcodes.put(newInst.getMnemonic(), newInst.getOpCode());
        return true;
    }

    public int getOpCode(String mnemonic) {
        Integer ret = registerOpcodes.get(mnemonic);
        if (ret == null) {
            Miscellaneous.printlnErr(mnemonic);
            new Exception().printStackTrace();
            Miscellaneous.exit();
        }
        return ret;
    }

    public boolean register(Instruction newInst, Instruction oldInst) {
        Vector vec = new Vector();
        vec.addElement(oldInst);
        return register(newInst, vec);
    }

    /**
     * Each optimization after creating a new instruction send this function
     * new-instruction and old-instruction-set (one or more old instructions).
     * The function use this input to create a map between old and new instructions.
     * 
     * @param newInst these new instructions are NOT valid Java bytecode instructions.
     * @param oldInstructions these are the instruction that are supported by java bytecode specifications.
     * @return
     */
    public boolean register(Instruction newInst, Vector<Instruction> oldInstructions) {
        if (oldInstructions.size() == 0 || newInst == null) {
            Miscellaneous.printlnErr(oldInstructions + ", " + newInst);
            throw new UnsupportedOperationException();
        }
        if (!saveAndGetOpcode(newInst, oldInstructions)) {
            return false;
        }
        dirty = true;
        newRegistration = true;
        //Miscellaneous.println(newInst.getOpCode()+":--------------- Registering instruction " +  newInst);
        if (oldInstructions.size() == 1) {
            if (recieveMultiInst) {
                Miscellaneous.printlnErr("registering of single instruction are not" +
                        " allowed after registering a few multiple instructions, " +
                        newInst);
                Miscellaneous.exit();
            }
            processSingleInstrOptim(newInst, oldInstructions.elementAt(0));
        } else {
            processMultiInstrsOptim(newInst, oldInstructions);
            produceStats(oldInstructions);
            recieveMultiInst = true;
        }
        return true;
    }

    private void produceStats(Vector oldInstructions) {
        totalNumberOfMultInstr++;
        totalLengthOfMultInstr += oldInstructions.size();
        if (oldInstructions.size() > maxSizeOfMultInstr) {
            maxSizeOfMultInstr = oldInstructions.size();
        }
    }

    public int getTotalNumberOfMultiInst() {
        return totalNumberOfMultInstr;
    }

    public long getTotalLengthOfMultiInst() {
        return totalLengthOfMultInstr;
    }

    public int getMaxSizeOfMultiInst() {
        return maxSizeOfMultInstr;
    }

    /**
     * -- Check if a opcode is used by a newInstruction then no oldInstruction should have 
     * use that same opcode
     * -- check the no opcode is greater than 255
     * -- 
     */
    private void validate() {
    }

    /**
     * Here we could have only two kinds of instructions
     * -1- opcode-operand combine in a new instruction or
     * -2- new insturction with a smaller operand.
     * In case new and old instruction has no operands or equal size operands 
     * then it  is an error and UnsupportedOperationException is thrown back.
     *
     * @param newInst
     * @param newInstr
     */
    private void processSingleInstrOptim(Instruction newInst, Instruction oldInst) {
        Un newOperand = newInst.getOperandsData();
        Un oldOperand = oldInst.getOperandsData();
        if ((newOperand != null && oldOperand == null) || (newOperand.size() > oldOperand.size())) {
            Miscellaneous.printlnErr("Invalid optimization new instruction is" +
                    " longer in size then original instruction");
            Miscellaneous.exit();
        }
        int sizeOfNewOperand = (newOperand == null ? 0 : newOperand.size());
        SingleInstrOptimRecord sIOR = new SingleInstrOptimRecord(oldInst.getOpCode(),
                sizeOfNewOperand, oldOperand, newInst);
        newInstrToSingleOldInstMap.put(newInst.getOpCode(), sIOR);
    }

    private void processMultiInstrsOptim(Instruction newInst,
            Vector<Instruction> oldInst) {
        int size = oldInst.size();

        Vector opCodeVector = new Vector();
        for (int loop = 0; loop < size; loop++) {
            opCodeVector.addElement(oldInst.elementAt(loop).getOpCode());
        }
        newInstrToOldInstsMap.put(newInst.getOpCode(),
                new MultiInstrOptimRecord(opCodeVector, newInst));
    }

    @Override
    public String toString() {
        return "\n\nmult-instr-map={" + newInstrToOldInstsMap.toString() + "},  " +
                "single-instr-map={" + newInstrToSingleOldInstMap.toString();
    }
}
