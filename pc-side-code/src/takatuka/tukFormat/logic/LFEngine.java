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
package takatuka.tukFormat.logic;

import takatuka.optimizer.cpGlobalization.logic.util.Oracle;
import takatuka.classreader.dataObjs.*;
import takatuka.classreader.dataObjs.constantPool.EmptyInfo;
import takatuka.classreader.dataObjs.constantPool.base.*;
import takatuka.classreader.logic.constants.*;
import takatuka.classreader.logic.factory.*;
import takatuka.classreader.logic.logAndStats.LogHolder;
import takatuka.classreader.logic.util.*;
import takatuka.tukFormat.dataObjs.*;
import takatuka.tukFormat.dataObjs.constantpool.*;
import takatuka.tukFormat.logic.factory.LFFactoryFacade;
import takatuka.tukFormat.logic.util.*;
import takatuka.optimizer.cpGlobalization.dataObjs.constantPool.GCP.*;
import takatuka.vm.autoGenerated.*;

/**
 * <p>Title: </p>
 * <p>Description:
 * Here is most of the logic of LoadingFormat
 * The logic is as following:
 * 1. Get the size of CP header and save it.
 * 2. Get the size of ConstantPool and save it.
 * 3. Now use contant Pool to mark some FieldInfo and MethodInfo as not referred.
 * 4. Calculate ClassFile addresses and set classFile addresses in corresponding classinfos.
 * 5. For ClassFile address is set in corresponding ClassInfo *
 * 6. Set address of MethodRefInfos
 * 7. Also set StringInfo addresses and mark corresponding UTF8Info as keep.
 * </p>
 * @author Faisal Aslam
 * @version 1.0
 */
public class LFEngine {

    private static final LFEngine engine = new LFEngine();
    private GlobalConstantPool pOne = GlobalConstantPool.getInstanceOf();
    private static boolean alwaysBigAddresses = false;
    private FactoryFacade factory = FactoryPlaceholder.getInstanceOf().getFactory();
    private static final SizeCalculator sizeCal = SizeCalculator.getInstanceOf();
    private int staticSize = 0;
    public static boolean removeDaglingReferences = false;

    private LFEngine() {
        super();
    }

    public static LFEngine getInstanceOf() {
        return engine;
    }

    public static void setAlwaysBigAddresses() {
        alwaysBigAddresses = true;
    }

    private void debugMsg(String str) {
        LogHolder.getInstanceOf().addLog(str);
    }

    private int getMaximumAddress() throws Exception {
        int totalSize = 0;
        CPHeader cpHeader = CPHeader.getInstanceof();
        //header size
        totalSize = SizeCalculator.getObjectSize(cpHeader);
        sizeCal.setAddress(totalSize, cpHeader);
        //cp size
        totalSize = sizeCal.cacluateAndCacheCPAddress(totalSize).
                intValueUnsigned();
        //class file size
        totalSize = ClassFileSizeCalculator.cacluateAndCacheClassFileAddress(totalSize).intValueUnsigned();
        return totalSize;
    }

    private void setAddressesTrimValue() throws Exception {
        int maxAddress = getMaximumAddress();
        int addressLength = 4;
        int toSubtract = (ClassFileController.getInstanceOf().getCurrentSize()) * 2;
        if ((maxAddress - toSubtract) < 65535 && !alwaysBigAddresses) {
            addressLength = 2; //we support only 2 and 4 byte address at the moment.
        }
        LFFactoryFacade.setTrimAddressValue(addressLength);
    }

    /**
     * Go to a classfile
     * Find a static field. And set its type and offset
     * Use that offset as an input for cacluating offset of next field
     * continue above process until all classfile are visited.
     */
    private void setStaticFieldsTypeAndOffset() throws Exception {
        ClassFileController cFileCont = ClassFileController.getInstanceOf();
        int size = cFileCont.getCurrentSize();
        FieldInfoController fieldCont = null;
        int staticFieldOffSet = 0;
        LFFieldInfo field = null;
        for (int classIndex = 0; classIndex < size; classIndex++) {
            ClassFile cFile = (ClassFile) cFileCont.get(classIndex);
            fieldCont = cFile.getFieldInfoController();
            for (int fieldIndex = 0; fieldIndex < fieldCont.getCurrentSize(); fieldIndex++) {
                field = (LFFieldInfo) fieldCont.get(fieldIndex);
                if (field.getAccessFlags().isStatic()) {
                    //Miscellaneous.println("abc = "+Oracle.getInstanceOf().methodOrFieldName(field, pOne));
                    staticFieldOffSet = LFClassFile.setFieldOffsetAndType(field, staticFieldOffSet);
                }
            }
        }
        if (field != null) {
            staticSize = staticFieldOffSet + LFClassFile.calculateFieldLength(field);
        }
    }

    public int getStaticSize() {
        return staticSize;
    }

    private void removeObjectWithDanglingReferences() {
        removeObjectWithDanglingReferences(TagValues.CONSTANT_Class);
        removeObjectWithDanglingReferences(TagValues.CONSTANT_Methodref);
    }

    private void removeObjectWithDanglingReferences(int tagValue) {
        try {
            int size = pOne.getCurrentSize(tagValue);
            for (int loop = 0; loop < size; loop++) {
                BaseObject baseObject = (BaseObject) pOne.get(loop, tagValue);
                if (baseObject instanceof EmptyInfo) {
                    continue;
                }
                int myValue = -1;
                if (tagValue == TagValues.CONSTANT_Class) {
                    myValue = ((LFClassInfo) baseObject).getClassFileAddress().intValueUnsigned();
                } else if (tagValue == TagValues.CONSTANT_Methodref) {
                    myValue = ((LFMethodRefInfo) baseObject).getFieldMethodInfoAddress().intValueUnsigned();
                }
                if (myValue == 0) {
                    //removeOnlyOnce = true;
                    //Miscellaneous.println("see me ------------------------> " + pOne.get(loop, tagValue));
                    pOne.replaceWithEmptyInfo(loop, tagValue);
                }
            }
        } catch (Exception d) {
            d.printStackTrace();
            Miscellaneous.exit();
        }
    }

    public void execute() throws Exception {
        int totalSize = 0;
        CPHeader cpHeader = CPHeader.getInstanceof();

        debugMsg("started generating loading format ...");

        //step 0.
        markUTF8WithCorrespondingStringInfos();

        //step 8 //Should be hear before we calculate the size of CP.
        setAllCPTagsToNull();

        for (int loop = 0; loop < 2; loop++) {

            //step 3.
            setCPRefAddressesWithFieldMethodInfo();

            debugMsg("init is done ...");

            //see if the addresses could be 2 or 4 byte long
            setAddressesTrimValue();

            //step 1.
            // cp header address
            totalSize = SizeCalculator.getObjectSize(cpHeader);

            sizeCal.setAddress(totalSize, cpHeader);

            debugMsg("cpheader size is calculated...");

//        addStaticMethodRefInfo();

            //step 2.
            //cp addresses
            totalSize = sizeCal.cacluateAndCacheCPAddress(totalSize).
                    intValueUnsigned();

            debugMsg("all CP info addresses are cacluated and cached...");

            //step 4.
            //class file addresses
            totalSize = ClassFileSizeCalculator.cacluateAndCacheClassFileAddress(totalSize).intValueUnsigned();

            // ------------------------------

            //step 5.
            //set class files adddress in class info
            setClassInfoSpecialAddresses();
            debugMsg("class info addresses are set...");

            //step 6.
            setFieldMethodRefInfoSpecialAddresses(false);
            setFieldMethodRefInfoSpecialAddresses(true);
            debugMsg("method and field addresses are set...");
            if (loop == 0 && removeDaglingReferences) {
                removeObjectWithDanglingReferences();
            } else {
                break;
            }
        }
        //step 7
        setStringInfoAddresses();
        debugMsg("string addresses are set...");


        //step 8 about statics
        setStaticFieldsTypeAndOffset();

        StaticCreator.getInstanceOf().generateHeaderFileWithClinitMethodAddress();
        debugMsg("static fields offset and types are set ...");

        //AddressVerifier.getInstanceOf().verifyCPAndHeaderAddresses();
    }


    public void setAllCPTagsToNull() throws Exception {
        InfoBase.printTag(false);
    }

    private void markUTF8WithCorrespondingStringInfos() throws Exception {
        LFStringInfo sInfo = null;
        LFUTF8Info utf8Info = null;
        for (int loop = 0; loop < pOne.getCurrentSize(TagValues.CONSTANT_String); loop++) {
            sInfo = (LFStringInfo) pOne.get(loop, TagValues.CONSTANT_String);
            utf8Info = (LFUTF8Info) pOne.get(sInfo.getIndex().intValueUnsigned(),
                    TagValues.CONSTANT_Utf8);
            utf8Info.setKeep(true);
        }
    }

    /**
     * Loop through all the StringInfo.
     * get UTF8Address and set it in StringInfo.
     *
     */
    private void setStringInfoAddresses() throws Exception {

        LFStringInfo sInfo = null;
        Un utf8Address = null;

        for (int loop = 0; loop < pOne.getCurrentSize(TagValues.CONSTANT_String); loop++) {
            sInfo = (LFStringInfo) pOne.get(loop, TagValues.CONSTANT_String);
            int cpIndex = sInfo.getIndex().intValueUnsigned();
            utf8Address = SizeCalculator.getCPAddress(cpIndex,
                    TagValues.CONSTANT_Utf8); //utf8 Index
            //System.out.println(loop+":  cp index = "+cpIndex+", address="+utf8Address);
            sInfo.setUTF8Address(utf8Address);
        }
    }

    /**
     * The function has following steps.
     * 1. Loop through all field and Method Reference Infos.
     * 2. For each refInfo. Get the class and NameAndType.
     * 3. Go to the class and check NameAndType in its all corresponding field-Method infos.
     * 4. If found then set that field-Method info as referred and set address of referred info accordingly
     * 5. Otherwise, go to superClass and repeat the same procedure.
     *
     * @param isField boolean
     */
    private void setFieldMethodRefInfoSpecialAddresses(boolean isField) throws
            Exception {
        int tag = TagValues.CONSTANT_Methodref;
        if (isField) {
            tag = TagValues.CONSTANT_Fieldref;
        }
        LFFieldRefInfo fieldInfo = null;
        for (int loop = 0; loop < pOne.getCurrentSize(tag); loop++) {
            BaseObject obj = (BaseObject) pOne.get(loop, tag);
            if (obj instanceof EmptyInfo) {
                continue;
            }
            fieldInfo = (LFFieldRefInfo) obj;
            setFieldMethodRefInfoSpecialAddress(loop, fieldInfo.getIndex(),
                    isField);
        }
    }

    private void setFieldMethodRefInfoSpecialAddress(int fromIndex,
            Un thisPointer, boolean isField) throws Exception {
        LFFieldRefInfo fieldInfo = (LFFieldRefInfo) pOne.get(fromIndex,
                isField ? TagValues.CONSTANT_Fieldref : TagValues.CONSTANT_Methodref);
        LFClassFile file = (LFClassFile) Oracle.getInstanceOf().getClass(thisPointer, pOne);
        if (file == null) {
            return; //may be file is not loaded
        }
        int index = 0;
        if ((index = file.findMethodField(fieldInfo.getNameAndTypeIndex(), isField)) !=
                -1) {
            Un address = file.getMethodOrFieldAddress(index, isField);
            fieldInfo.setFieldMethodInfoAddress(address);
        } else if (file.getSuperClass() != null &&
                file.getSuperClass().intValueUnsigned() != 0) { //not an Object class
            setFieldMethodRefInfoSpecialAddress(fromIndex, file.getSuperClass(),
                    isField);
        }
    }

    private void setClassInfoSpecialAddresses() throws Exception {
        ClassFileController cont = ClassFileController.getInstanceOf();
        LFClassFile file = null;
        LFClassInfo info = null;
        for (int loop = 0; loop < cont.getCurrentSize(); loop++) {
            file = (LFClassFile) cont.get(loop);
            info = (LFClassInfo) pOne.get(file.getThisClass().intValueUnsigned(),
                    TagValues.CONSTANT_Class);
            info.setClassFileAddress(file.getAddress());
        }
    }

//-----------------------------------------------------------------------
    /**
     * 
     */
    private void setCPRefAddressesWithFieldMethodInfo() throws Exception {
        ClassFileController cont = ClassFileController.getInstanceOf();
        LFClassFile file = null;
        MethodInfoController mcontr = null;
        FieldInfoController fcontr = null;
        for (int loop = 0; loop < cont.getCurrentSize(); loop++) {
            file = (LFClassFile) cont.get(loop);
            mcontr = file.getMethodInfoController();
            setCPRefAddressesWithFieldMethodInfo(mcontr, file, true);
            fcontr = file.getFieldInfoController();
            setCPRefAddressesWithFieldMethodInfo(fcontr, file, false);
        }
    }

    private void setCPRefAddressesWithFieldMethodInfo(ControllerBase cont,
            LFClassFile file, boolean isMethod) throws
            Exception {
        int ret = -1;
        LFFieldInfo field = null;
        Oracle oracle = Oracle.getInstanceOf();
        for (int index = 0; index < cont.getCurrentSize(); index++) {
            field = (LFFieldInfo) cont.get(index);
            ret = oracle.existFieldInfoCPIndex(field, isMethod,
                    file.getThisClass().intValueUnsigned());
            //debugMsg(" while marking "+field.getName()+", "+file.getFullyQualifiedClassName()+", index = "+ret);
            field.setReferanceIndex(ret);
        }
    }
}
