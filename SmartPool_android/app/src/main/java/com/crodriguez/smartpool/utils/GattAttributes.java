/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crodriguez.smartpool.utils;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    private static final String UUID_MASK = "0000%s-0000-1000-8000-73a310a5bbfd";
//    public static String HM_RX_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public static String UUID_SERVICE_DATOS          = String.format(UUID_MASK, "ffd0");
    public static String UUID_SERVICE_DATOS1          = String.format(UUID_MASK, "ffa0");
    public static String UUID_CHARACTERISTIC_TX      = String.format(UUID_MASK,"ffe9");
    public static String UUID_CHARACTERISTIC_RX      = String.format(UUID_MASK,"ffd9");
    public static String UUID_CHARACTERISTIC_TX_TEMP     = String.format(UUID_MASK, "ffe1");
    public static String UUID_CHARACTERISTIC_RX_TEMP     = String.format(UUID_MASK, "ffd1");
    public static String UUID_CHARACTERISTIC_TX_PH      = String.format(UUID_MASK,"ffe2");
    public static String UUID_CHARACTERISTIC_RX_PH     = String.format(UUID_MASK, "ffd2");
    public static String UUID_CHARACTERISTIC_TX_DATE     = String.format(UUID_MASK, "ffe3");
    public static String UUID_CHARACTERISTIC_RX_DATE     = String.format(UUID_MASK, "ffd3");
    public static String UUID_CHARACTERISTIC_TX_TURBI      = String.format(UUID_MASK,"ffe4");
    public static String UUID_CHARACTERISTIC_RX_TURBI      = String.format(UUID_MASK,"ffd4");
    public static String UUID_CHARACTERISTIC_TX_NAME      = String.format(UUID_MASK,"ffe5");
    public static String UUID_CHARACTERISTIC_RX_NAME      = String.format(UUID_MASK,"ffd5");




    public static String CLIENT_CHARACTERISTIC_CONFIG = String.format(UUID_MASK, "2902");
    public static String UUID_CHARACTERISTIC_DEVICE_NAME = String.format(UUID_MASK, "2a00");
    public static String UUID_CHARACTERISTIC_RECONNECTION_ADDRESS = String.format(UUID_MASK, "2a03");
    public static String UUID_CHARACTERISTIC_MANUFACTURE_NAME    = String.format(UUID_MASK, "2a29");

    static {
        // Sample Services.
        attributes.put(UUID_SERVICE_DATOS, "Servicio 1");
        attributes.put(UUID_SERVICE_DATOS1, "Servicio 2");
//        attributes.put(HM_RX_TX,"RX/TX data");
//        attributes.put(String.format(UUID_MASK, "2a29"), "Manufacturer Name String");
//
//        // Sample Characteristics.
//        attributes.put(String.format(UUID_MASK, "180a"), "Device Information Service");
//
//        attributes.put(UUID_CHARACTERISTIC_TX,"TX data");
//        attributes.put(UUID_CHARACTERISTIC_TX_LUZ, "Luz tx");
//        attributes.put(UUID_CHARACTERISTIC_RX_LUZ, "Luz rx");
//        attributes.put(UUID_CHARACTERISTIC_TX_CAL, "Calefaccion tx");
//        attributes.put(UUID_CHARACTERISTIC_RX_CAL, "Calefaccion rx");
//        attributes.put(UUID_CHARACTERISTIC_TX_POS, "Posicion tx");
//        attributes.put(UUID_CHARACTERISTIC_RX_POS, "Posicion rx");
//        attributes.put(UUID_CHARACTERISTIC_TX_NAME,"Name TX");
//        attributes.put(UUID_CHARACTERISTIC_RX_NAME,"Name RX");
//
//
//
//        attributes.put(UUID_CHARACTERISTIC_MANUFACTURE_NAME, "Manufacturer Name String");
//        attributes.put(UUID_CHARACTERISTIC_DEVICE_NAME, "Device Name");
//        attributes.put(UUID_CHARACTERISTIC_RECONNECTION_ADDRESS, "Reconnection Address");


    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
