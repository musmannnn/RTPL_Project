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
package takatuka.vm.autoGenerated.sizesStat;

import java.util.*;
import takatuka.classreader.dataObjs.*;
import takatuka.tukFormat.dataObjs.*;

/**
 * <p>Title: </p>
 * <p>Description:
 *
 * </p>
 * @author Faisal Aslam
 * @version 1.0
 */
class BaseObjectSizeController {

    private TreeSet<BaseObjectSize> classesSizes = new TreeSet<BaseObjectSize>();
    private TreeSet<BaseObjectSize> methodsSizes = new TreeSet<BaseObjectSize>();
    private static BaseObjectSizeController controller = new BaseObjectSizeController();

    /**
     * 
     * @return
     */
    public static BaseObjectSizeController getInstanceOf() {
        return controller;
    }

    /**
     *
     * @param classFile
     * @param size
     */
    public void addSize(LFClassFile classFile, int size) {
        //Miscellaneous.println("I am called ");

        BaseObjectSize boSize = new BaseObjectSize(classFile, size);
        classesSizes.add(boSize);

    }

    /**
     * 
     * @param methodInfo
     * @param size
     */
    public void addSize(LFMethodInfo methodInfo, int size) {
        //Miscellaneous.println("I am called ");
        BaseObjectSize boSize = new BaseObjectSize(methodInfo, size);
        methodsSizes.add(boSize);
    }

    private String writeTree(TreeSet<BaseObjectSize> tree) {
        String ret = "";
        Iterator<BaseObjectSize> it = tree.iterator();
        while (it.hasNext()) {
            BaseObjectSize baseSize = it.next();
            ret = ret + baseSize + "\n\n";
        }
        return ret;
    }

    @Override
    public String toString() {
        String ret = "";
        //       Miscellaneous.println("I am called "+classesSizes.size()+" , "+methodsSizes.size());
        ret += "*** This file contains actual sizes of all the library and user application files ***\n";
        ret += "*** These are the only classfiles (and methods) that are transferred to the mote ***\n";
        ret += "***************** Class File Sizes (bytes) *********** \n";
        ret += writeTree(classesSizes);
        ret += "\n\n\n****************************************** \n";
        ret += "***************** Method Sizes (bytes) *********** \n\n";
        ret += writeTree(methodsSizes);
        return ret;
    }
}