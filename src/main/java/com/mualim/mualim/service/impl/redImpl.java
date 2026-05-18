package com.mualim.mualim.service.impl;

import com.mualim.mualim.service.Red;

import org.springframework.stereotype.Component;

@Component
public class redImpl implements Red {


    @Override
    public String print() {return "red";}
}
