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
package takatuka.chunkSizeCalc;

import java.io.*;
import java.util.*;
import takatuka.classreader.dataObjs.*;
import takatuka.classreader.dataObjs.attribute.*;
import takatuka.classreader.logic.*;
import takatuka.classreader.logic.file.*;
import takatuka.classreader.logic.logAndStats.*;
import takatuka.classreader.logic.util.*;
import takatuka.offlineGC.DFA.dataObjs.flowRecord.*;
import takatuka.offlineGC.DFA.dataObjs.virtualThreading.*;
import takatuka.offlineGC.DFA.dataObjs.attribute.*;
import takatuka.optimizer.VSS.logic.preCodeTravers.*;
import takatuka.optimizer.cpGlobalization.logic.util.*;
import takatuka.tukFormat.dataObjs.LFMethodInfo;
import takatuka.vm.autoGenerated.*;

/**
 * The Compile time variable chunk scheme (CVCS) information is generated
 * using 
 * 
 * @author Faisal Aslam
 */
public class GenerateCVCSInfo {

    public static String cvcsInputFileName = null;
    private static final String CVCS_OUTPUT_FILE = "CVCS_FILE_NAME";
    private static final String CVCS_FRAME_FIXED_OVERHEAD = "CVCS_FRAME_FIXED_OVERHEAD";
    public static int depthForCVCS = 3;
    public static boolean generateCVCSInfo = false;
    private static final int CHUNK_FOR_VM_METHODS = 0;
    private int maxCVCSEntry = CHUNK_FOR_VM_METHODS;
    private double totalCVCSEntriesSum = 0;
    private double numberOfCVCSEntries = 0;
    private boolean calledWriteInFiles = false;
    private static final String LOG_FILE = "cvcs_log.txt";
    public static int maxChunkSize = -1;
    private static final String VM_INIT = "takatuka.vm.VM.initialize";
    private static final int VM_INIT_CHUNK_SIZE = 200;
    private HashSet<Integer> alreadyVisitedCPIndexes = new HashSet<Integer>();
    /**
     * could be both positive and negative number
     */
    public static double CHANGE_DEPTH = 1;
    private double maxDepth = 0;
    //private static final int CHUNK_FOR_VM_METHODS = 0;
    /**
     * method id is key, value is chunk size and method.
     */
    private TreeMap<Integer, MethodAndChunkSize> record = new TreeMap<Integer, MethodAndChunkSize>();
    private Stack<String> logStack = new Stack<String>();

    public GenerateCVCSInfo() {
    }

    public int getBytesToStoreCVSEntries() {
        if (!calledWriteInFiles) {
            return 2;
        }
        LogHolder.getInstanceOf().addLog("Maximum chunk size =" + maxCVCSEntry
                + ", average size="
                + StartMeAbstract.roundDouble(totalCVCSEntriesSum / numberOfCVCSEntries, 2),
                true);
        LogHolder.getInstanceOf().addLog("Max CVCS depth =" + maxDepth, true);
        if (maxCVCSEntry > 255) {
            return 2;
        } else {
            return 1;
        }
    }

    private void addLog(String str) {
        logStack.push(str);
        // LogHolder.getInstanceOf().addLog(str, LOG_FILE, false);
    }

    private void purgeLog() {
        LogHolder.getInstanceOf().addLog("\n*************************************\n", LOG_FILE, false);
        while (!logStack.empty()) {
            LogHolder.getInstanceOf().addLog(logStack.pop(), LOG_FILE, false);
        }
    }

    private void writeInFile() {
        try {
            calledWriteInFiles = true;
            String fileName = ConfigPropertyReader.getInstanceOf().getConfigProperty(CVCS_OUTPUT_FILE);
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
            Oracle oracle = Oracle.getInstanceOf();
            int totalMethods = oracle.getAllMethodInfo().size();
            RandomAccessFile rm = new RandomAccessFile(file, "rw");
            String strForFile = HeaderFileConstants.AUTO_GENERATED_MSG;//
            strForFile += "{\n";
            Set<Integer> keys = record.keySet();
            Iterator<Integer> keysIt = keys.iterator();
            int lastKey = -1;
            while (keysIt.hasNext()) {
                int key = keysIt.next();
                if (lastKey != -1 && lastKey + 1 != key) {
                    int temp = lastKey;
                    while (temp != key - 1) {
                        temp++;
                        strForFile += CHUNK_FOR_VM_METHODS + ",\n";
                    }
                    //System.err.println("error # 434 ");
                    //System.exit(1);
                }
                lastKey = key;
                MethodAndChunkSize methodChunkSize = record.get(key);
                numberOfCVCSEntries++;
                totalCVCSEntriesSum += methodChunkSize.chunkSize;
                if (maxCVCSEntry < methodChunkSize.chunkSize) {
                    maxCVCSEntry = methodChunkSize.chunkSize;
                }
                strForFile += methodChunkSize.toString();
                if (keysIt.hasNext()) {
                    strForFile += ",\n";
                } else {
                    strForFile += ",";
                }
            }
            int endLoop = 10;
            for (int loop = 0; loop < endLoop; loop++) {
                strForFile += "\n";
                strForFile += CHUNK_FOR_VM_METHODS + "";
                if (loop + 1 < endLoop) {
                    strForFile += ",";
                }
            }
            strForFile += "\n};\n";
            rm.writeBytes(strForFile);

        } catch (Exception d) {
            d.printStackTrace();
            System.exit(1);
        }

    }

    private void addRecord(int methodCPIndex, LFMethodInfo lfmethodInfo, int chunkSize) {
        MethodAndChunkSize lastValue = record.put(methodCPIndex, new MethodAndChunkSize(lfmethodInfo, chunkSize));
        if (lastValue != null) {
            System.err.println("Error 672  " + lastValue);
            System.exit(1);
        }
    }

    private static void copyfile(String srFile, String dtFile) {
        try {
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            if (f2.exists()) {
                //f2.delete();
            }
            InputStream in = new FileInputStream(f1);
            //For Overwrite the file.
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * For each thread:
     * Get its main method m.
     * For m.
     * Check what are the functions invoked from m.
     * First go through all the methods of m
     * Calculate max-stack and max-local at the invoke instruction
     * Calculate method 
     */
    public void execute() {
        try {
            if (cvcsInputFileName != null) {
                String cvcsOutFileName = ConfigPropertyReader.getInstanceOf().getConfigProperty(CVCS_OUTPUT_FILE);
                System.out.println("copying file to " + cvcsOutFileName);
                copyfile(cvcsInputFileName, cvcsOutFileName);
            }
            if (!generateCVCSInfo) {
                return;
            }
            HashMap<String, CodeAttCache> codeAttCacheMap = getAllCodeAttCache();
            LogHolder.getInstanceOf().addLog("Size of CVCS Depth=" + depthForCVCS, true);
            VirtualThreadController vContr = VirtualThreadController.getInstanceOf();
            Collection<VirtualThread> vThreadCollection = vContr.getAllFinishedThreads();
            Iterator<VirtualThread> it = vThreadCollection.iterator();
            while (it.hasNext() && depthForCVCS != -1) {
                VirtualThread vThread = it.next();
                traverse(vThread, codeAttCacheMap);
            }
            Iterator<String> keysIt = codeAttCacheMap.keySet().iterator();
            while (keysIt.hasNext()) {
                String key = keysIt.next();

                CodeAttCache codeAttCache = codeAttCacheMap.get(key);
                //System.out.println("found function = " + key);
                LFMethodInfo lfmethodInfo = (LFMethodInfo) codeAttCache.getMethodInfo();
                int methodCPIndex = LFMethodInfo.computeRefInfoUsingInfo(lfmethodInfo);
                if (key.startsWith(VM_INIT)) {
                    addRecord(methodCPIndex, lfmethodInfo, VM_INIT_CHUNK_SIZE);
                    //record.put(methodCPIndex, new MethodAndChunkSize(lfmethodInfo, VM_INIT_CHUNK_SIZE));
                } else {
                    addRecord(methodCPIndex, lfmethodInfo, CHUNK_FOR_VM_METHODS);
                    //record.put(methodCPIndex, new MethodAndChunkSize(lfmethodInfo, CHUNK_FOR_VM_METHODS));
                }
            }
            writeInFile();
        } catch (Exception d) {
            d.printStackTrace();
            System.exit(1);
        }
    }

    private HashMap<String, CodeAttCache> getAllCodeAttCache() {
        HashMap<String, CodeAttCache> ret = new HashMap<String, CodeAttCache>();
        Oracle oracle = Oracle.getInstanceOf();
        Iterator<CodeAttCache> codeAttCacheIt = oracle.getAllCodeAtt().iterator();
        while (codeAttCacheIt.hasNext()) {
            CodeAttCache codeAttCache = codeAttCacheIt.next();
            String methodStr = oracle.getMethodOrFieldString(codeAttCache.getMethodInfo());
            ret.put(methodStr, codeAttCache);
        }
        return ret;
    }

    private MethodCallInfo getMainMethodCallInfo(MethodInfo mainMethod) {
        FunctionsFlowRecorder flowRecorder = FunctionsFlowRecorder.getInstanceOf();
        HashSet<MethodCallInfo> mainMethodFlowNodeSet = flowRecorder.getMainMethods();
        Iterator<MethodCallInfo> mainMethodFlowIt = mainMethodFlowNodeSet.iterator();
        MethodCallInfo mainMethodFlow = null;
        Oracle oracle = Oracle.getInstanceOf();
        while (mainMethodFlowIt.hasNext()) {
            MethodCallInfo funFlowNode = mainMethodFlowIt.next();
            if (oracle.getMethodOrFieldString(mainMethod).equals(oracle.getMethodOrFieldString(funFlowNode.getMethod()))) {
                mainMethodFlow = funFlowNode;
                break;
            }
        }
        if (mainMethodFlow == null) {
            Miscellaneous.printlnErr("Cannot find the thread main method. Error # 68923 ");
            Miscellaneous.exit();
        }
        return mainMethodFlow;
    }

    /**
     * This function traverse in all the methodCallInfos
     * 
     * @param vthread
     * @param codeAttCache
     * @throws Exception 
     */
    private void traverse(VirtualThread vthread, HashMap<String, CodeAttCache> codeAttCache) throws Exception {

        /**
         * Get the main methods of the thread.
         */
        MethodInfo mainMethod = vthread.getStartingMethod();

        /**
         * Get the list of main method of all the threads and from them find
         * above main method.
         */
        MethodCallInfo mainMethodFlow = getMainMethodCallInfo(mainMethod);
        Stack<MethodCallInfoAndItsDepth> stack = new Stack<MethodCallInfoAndItsDepth>();
        stack.push(new MethodCallInfoAndItsDepth(mainMethodFlow, 0));
        HashMap<MethodCallInfo, ChunkInfo> cacheForPerformance = new HashMap<MethodCallInfo, ChunkInfo>();
        while (!stack.empty()) {
            MethodCallInfoAndItsDepth mInfoAndItDepth = stack.pop();
            MethodCallInfo mInfo = mInfoAndItDepth.methodCallInfo;
            Oracle oracle = Oracle.getInstanceOf();
            String methodStr = oracle.getMethodOrFieldString(mInfo.getMethod());
            if (methodStr.startsWith("java.lang.Object.<init>()V")) {
                //System.out.println("Stop ");
            }
            LFMethodInfo lfmethodInfo = (LFMethodInfo) mInfo.getMethod();
            int methodCPIndex = LFMethodInfo.computeRefInfoUsingInfo(lfmethodInfo);
            if (alreadyVisitedCPIndexes.contains(methodCPIndex)) {
                continue;
            }
            alreadyVisitedCPIndexes.add(methodCPIndex);

            //System.out.println("current method string = " + methodStr + ", visitedBefore =" + mInfo.visitedBefore);
            MethodCallInfo.visitId2++;
            cacheForPerformance = new HashMap<MethodCallInfo, ChunkInfo>();
            if (methodStr.startsWith("java.")) {
                //System.out.println("********************* "+methodStr);
                //continue;
            }
            int chunkSizeForMethod = calculateChunkSize(mInfo, mInfoAndItDepth.depth,
                    cacheForPerformance);
            purgeLog();
            //System.out.println("+++++++ " + chunkSizeForMethod + "++++ " + methodStr);

            addRecord(methodCPIndex, lfmethodInfo, chunkSizeForMethod);
            //record.put(methodCPIndex, new MethodAndChunkSize(lfmethodInfo, chunkSizeForMethod));

            codeAttCache.remove(methodStr);
            /**
             * Now traverse throw its invoke instructions.
             */
            Iterator<MethodCallInfo> it = mInfo.getAllChildren().iterator();
            while (it.hasNext()) {
                MethodCallInfo child = it.next();
                LFMethodInfo childMethod = (LFMethodInfo) child.getMethod();
                //String childMethodStr = oracle.getMethodOrFieldString(childMethod);
                methodCPIndex = LFMethodInfo.computeRefInfoUsingInfo(childMethod);
                //System.out.println("child method string = " + childMethodStr + ", " + child.visitedBefore);
                if (alreadyVisitedCPIndexes.contains(methodCPIndex)) {
                    continue;
                }
                stack.push(new MethodCallInfoAndItsDepth(child, mInfoAndItDepth.depth + CHANGE_DEPTH));
            }
        }
    }

    /**
     * This function calculates chunk size for each method
     * 
     * Cfoo = Max (Ffoo, If+CIi) for all i and given level.
     * 
     * @param mInfo
     * @param currentDepth
     * @return 
     */
    private int calculateChunkSize(MethodCallInfo mInfo, double currentDepth,
            HashMap<MethodCallInfo, ChunkInfo> alreadyVisited) throws Exception {
        Oracle oracle = Oracle.getInstanceOf();
        if (mInfo.visitedBefore2 == MethodCallInfo.visitId2) {
            //return 0;
        }
        mInfo.visitedBefore2 = MethodCallInfo.visitId2;
        String methodStr = oracle.getMethodStringShort(mInfo.getMethod());
        String methodLongString = oracle.getMethodOrFieldString(mInfo.getMethod());
        if (methodLongString.equals("jvmTestCases.Printer.println(J)V")) {
            // System.out.println("stop here");
        }
        int Ffoo = getMethodFrameSize(mInfo);


        if (currentDepth > maxDepth) {
            maxDepth = currentDepth;
        }

        if (!alreadyVisited.containsKey(mInfo)) {

            //System.out.println("\t\t inside= " + methodStr);

            HashMap<Long, InstrAndFrameSizeTillInstr> instrToFrameSizeMap = getMaxFrameSizeTillInstruction(mInfo);
            Iterator<Long> InstrIdskeysIt = instrToFrameSizeMap.keySet().iterator();
            int ret = 0;
            int frameTillInstr = 0;
            String forLog = "";
            String temp = "";
            int logMaxChildFrame = 0;
            if (currentDepth < depthForCVCS) {
                ret = Ffoo;
                while (InstrIdskeysIt.hasNext()) {
                    Long instrId = InstrIdskeysIt.next();
                    InstrAndFrameSizeTillInstr value = instrToFrameSizeMap.get(instrId);
                    int maxChildChunk = 0;
                    Iterator<MethodCallInfo> children = mInfo.getChildren(instrId).iterator();
                    while (children.hasNext()) {
                        MethodCallInfo child = children.next();

                        maxChildChunk = Math.max(maxChildChunk, calculateChunkSize(child,
                                currentDepth + 1, alreadyVisited));
                        temp = oracle.getMethodStringShort(child.getMethod());
                        //System.out.println(" \t\t method-str " + methodStr + ", maxChildChunk =" + maxChildChunk);
                    }
                    ret = Math.max(ret, value.frameSize + maxChildChunk);
                    /**
                     * following code is for log only.
                     */
                    if (frameTillInstr < (value.frameSize + maxChildChunk)) {
                        frameTillInstr = value.frameSize + maxChildChunk;
                        logMaxChildFrame = value.frameSize;
                        forLog = temp;
                    }
                }
            }
            String logStr = "";
            for (int loop = 0; loop <= currentDepth; loop++) {
                logStr += "\t";
            }
            logStr += "\n" + logStr + "(" + logMaxChildFrame + ", " + forLog + ")" + " :" + methodStr + ": chunk=" + ret + ": Frame=" + Ffoo + ": depth=" + currentDepth;

            ChunkInfo cInfo = new ChunkInfo();
            if (ret > maxChunkSize && maxChunkSize != -1) {
                ret = maxChunkSize;
            }
            cInfo.chunkSize = ret;
            cInfo.depth = currentDepth;
            cInfo.logStr = logStr;
            alreadyVisited.put(mInfo, cInfo);
        }
        ChunkInfo cInfo = alreadyVisited.get(mInfo);
        addLog(cInfo.logStr);
        return cInfo.chunkSize;
    }

    /**
     * @param mInfo
     * @return 
     */
    static int getMethodFrameSize(MethodCallInfo mInfo) {
        try {
            LFMethodInfo method = (LFMethodInfo) mInfo.getMethod();
            CodeAtt codeAtt = method.getCodeAtt();
            return getMethodFrameSize(method, codeAtt);
        } catch (Exception d) {
            d.printStackTrace();
            System.exit(1);
        }
        return 0;
    }

    static int getMethodFrameSize(LFMethodInfo method, CodeAtt codeAtt) {
        int frameSize = 0;
        if (codeAtt == null) {
            return 0;
        }
        int maxStack = codeAtt.getMaxStack().intValueUnsigned();
        int maxLocals = codeAtt.getMaxLocals().intValueUnsigned();
        frameSize += calculateOverheadPerFrame(codeAtt);
        frameSize += (maxStack * ReduceTheSizeOfLocalVariables.BLOCK_SIZE_IN_BYTES)
                + (maxLocals * ReduceTheSizeOfLocalVariables.BLOCK_SIZE_IN_BYTES);

        return frameSize;
    }

    private static int calculateOverheadPerFrame(CodeAtt codeAtt) {
        String fixedOverheadString = ConfigPropertyReader.getInstanceOf().
                getConfigProperty(CVCS_FRAME_FIXED_OVERHEAD);
        int fixOverhead = Integer.parseInt(fixedOverheadString);
        int maxStack = codeAtt.getMaxStack().intValueUnsigned();
        int maxLocal = codeAtt.getMaxLocals().intValueSigned();
        double totalFrameSize = maxStack + maxLocal;
        totalFrameSize = totalFrameSize + fixOverhead;
        return (int) totalFrameSize;
    }

    /**
     * Get method invoke instructions 
     * and calculate max stack at that instruction
     * 
     * @param methodCallInfo
     * @return 
     */
    private HashMap<Long, InstrAndFrameSizeTillInstr> getMaxFrameSizeTillInstruction(MethodCallInfo methodCallInfo) {
        HashMap<Long, InstrAndFrameSizeTillInstr> ret =
                new HashMap<Long, InstrAndFrameSizeTillInstr>();
        Collection<GCInstruction> invokeInstrCollection =
                InvokeInstructionRecord.getInstanceOf().getInvokeInstrRecord(methodCallInfo);
        Iterator<GCInstruction> invokeIt = invokeInstrCollection.iterator();
        CodeAtt codeAtt = methodCallInfo.getMethod().getCodeAtt();
        int localMaxSize = 0;
        if (codeAtt != null) {
            localMaxSize = codeAtt.getMaxLocals().intValueUnsigned()
                    * ReduceTheSizeOfLocalVariables.BLOCK_SIZE_IN_BYTES;
        }
        while (invokeIt.hasNext()) {
            GCInstruction invokeInstr = invokeIt.next();
            Oracle oracle = Oracle.getInstanceOf();
            MethodInfo method = methodCallInfo.getMethod();
            String methodStr = oracle.getMethodOrFieldString(method);
            int methodTotalFrameSize = getMethodFrameSize(methodCallInfo);
            int stackMaxSize = -1;
            try {
                /*int localMaxSize = Math.max(invokeInstr.getLeavingLocalVariables().getCurrentSize(),
                invokeInstr.getLocalVariables().getCurrentSize());*/
                stackMaxSize = Math.max(invokeInstr.getLeavingOperandStack().getCurrentSize(),
                        invokeInstr.getOperandStack().getCurrentSize()) * ReduceTheSizeOfLocalVariables.BLOCK_SIZE_IN_BYTES;
            } catch (NullPointerException d) {

                LogHolder.getInstanceOf().addLog("null stack********** method=" +
                        methodStr + " invoke instr =" + invokeInstr, true);
            }
            int frameSize = localMaxSize + stackMaxSize + calculateOverheadPerFrame(codeAtt);
            if (stackMaxSize == -1) {
                frameSize = methodTotalFrameSize;
            }
            //System.out.println("adding " + invokeInstr.getInstructionId() + "-->" + new InstrAndFrameSizeTillInstr(invokeInstr, frameSize));
            if (frameSize > methodTotalFrameSize) {
                System.err.println("method FrameSize Till instr " + frameSize + " cannot be greater than method total frame size=" + methodTotalFrameSize);
                System.exit(1);
            }
            ret.put(invokeInstr.getInstructionId(), new InstrAndFrameSizeTillInstr(invokeInstr, frameSize));

        }
        return ret;
    }
}
