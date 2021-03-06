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
package takatuka.vm.autoGenerated;

import java.util.*;
import takatuka.classreader.dataObjs.*;
import takatuka.classreader.logic.constants.*;
import takatuka.classreader.logic.factory.*;
import takatuka.classreader.logic.file.*;
import takatuka.classreader.logic.util.*;
import takatuka.optimizer.cpGlobalization.dataObjs.constantPool.GCP.*;
import takatuka.optimizer.cpGlobalization.logic.util.*;
import takatuka.tukFormat.dataObjs.*;
import takatuka.tukFormat.dataObjs.constantpool.*;

/**
 * <p>Title: </p>
 * <p>Description:
 * It generates map of all the array clases
 * </p>
 * @author Faisal Aslam
 * @version 1.0
 */
public class ArrayClassesMap {

    private static final String ARRAY_CLASSES_MAP = "ARRAY_CLASSES_MAP";
    private static final ArrayClassesMap myObj = new ArrayClassesMap();
    private static final ConfigPropertyReader configPropReader =
            ConfigPropertyReader.getInstanceOf();
    private static final String BYTE_ARRAY = "[B";
    private static final String CHAR_ARRAY = "[C";
    private static final String DOUBLE_ARRAY = "[D";
    private static final String FLOAT_ARRAY = "[F";
    private static final String INT_ARRAY = "[I";
    private static final String LONG_ARRAY = "[J";
    private static final String SHORT_ARRAY = "[S";
    private static final String BOOLEAN_ARRAY = "[Z";
    private Vector<String> primitiveTypesArray = new Vector<String>();
    public static int arrayClasses = 0;
    private static Oracle oracle = Oracle.getInstanceOf();
    private static FactoryFacade factory = FactoryPlaceholder.getInstanceOf().getFactory();

    public static ArrayClassesMap getInstanceOf() {
        return myObj;
    }

    public Vector execute() {
        try {
            addAllPrimativeTypes();
            writeInHeaderClinitTable();
        } catch (Exception d) {
            d.printStackTrace();
            Miscellaneous.exit();
        }
        return primitiveTypesArray;
    }

    private void writeInHeaderClinitTable() throws Exception {
        TreeMap<Integer, ClassFile> table = new TreeMap<Integer, ClassFile>();
        String fileName = configPropReader.getConfigProperty(ARRAY_CLASSES_MAP);
        if (fileName == null) {
            Miscellaneous.printlnErr("Cannot file configuration property " + ARRAY_CLASSES_MAP);
            Miscellaneous.exit();
        }
        LFRevUn.bigEndian = true;
        HeaderFileConstants.writeHeaderFile(createArrayClassesMap(table),
                fileName);
        LFRevUn.bigEndian = false;

    }

    private void addAllPrimativeTypes() {
        primitiveTypesArray.addElement(BYTE_ARRAY);
        primitiveTypesArray.addElement(CHAR_ARRAY);
        primitiveTypesArray.addElement(DOUBLE_ARRAY);
        primitiveTypesArray.addElement(FLOAT_ARRAY);
        primitiveTypesArray.addElement(INT_ARRAY);
        primitiveTypesArray.addElement(LONG_ARRAY);
        primitiveTypesArray.addElement(SHORT_ARRAY);
        primitiveTypesArray.addElement(BOOLEAN_ARRAY);
    }

    /**
     * Go to the end of classInfo constant pool.
     *
     * @param table
     * @return
     */
    private String createArrayClassesMap(TreeMap<Integer, ClassFile> table) {
        GlobalConstantPool gcp = GlobalConstantPool.getInstanceOf();

        int classFileSize = gcp.getCurrentSize(TagValues.CONSTANT_Class);
        for (int loop = classFileSize - 1; loop > 0; loop--) {
            LFClassInfo cInfo = (LFClassInfo) gcp.get(loop, TagValues.CONSTANT_Class);
            String className = cInfo.getClassName();
            ClassFile cFile = null;
            if (className.trim().startsWith("[")) {
                arrayClasses++;
                //Miscellaneous.println("------------- See me 3 "+className);
                if (!primitiveTypesArray.remove(className) && className.indexOf("[L") != -1) {
                    //Miscellaneous.println("------------- See me 2 "+className);
                    className = className.substring(className.indexOf("[L") + 2, className.length() - 1);
                    //Miscellaneous.println("------------- See me 1 "+className);
                    cFile = oracle.getClass(className);

                }
                table.put(loop, cFile);
            } else {
                break;
            }
        }
        return toStringArrayClassMap(table);
    }

    private String toStringArrayClassMap(TreeMap<Integer, ClassFile> table) {
        String ret = "{\n";
        try {
            Collection set = table.values();
            Iterator<LFClassFile> it = set.iterator();
            while (it.hasNext()) {
                LFClassFile cFile = it.next();
                //if class is not in tuk file (may be due to dead-code removal)
                if (cFile == null) {
                    ret += "0x" + factory.createUn(0).trim(2) + ",\n";
                } else {
                    ret += "0x" + cFile.getThisClass() + ",/*" + cFile.getFullyQualifiedClassName() + "*/\n";
                }

            }
        } catch (Exception d) {
            d.printStackTrace();
            Miscellaneous.exit();
        }
        ret += "};";
        return ret;

    }
}
