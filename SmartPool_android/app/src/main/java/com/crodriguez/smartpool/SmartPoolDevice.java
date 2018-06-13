package com.crodriguez.smartpool;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Clase para crear Objetos de dispositivos
 */

public class SmartPoolDevice implements Serializable {
    private int id_divice;
    private String name,mac;

    public SmartPoolDevice(int id_divice,String name,String mac) {
        this.id_divice = id_divice;
        this.name = name;
        this.mac = mac;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId_divice() {

        return id_divice;
    }

    public void setId_divice(int id_divice) {
        this.id_divice = id_divice;
    }

}
