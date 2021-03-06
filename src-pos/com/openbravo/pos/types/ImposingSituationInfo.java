/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.openbravo.pos.types;

import com.openbravo.data.loader.IKeyed;

/**
 *
 * @author ariel
 */
public class ImposingSituationInfo implements IKeyed {

    Integer id;
    String name;

    public ImposingSituationInfo(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object getKey() {
        return this.id;
    }

}
