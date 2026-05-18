package com.mualim.mualim.service.impl;

import com.mualim.mualim.service.Blue;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class blueImpl implements Blue {
    @Override
    public String print() {return "blue";}
}
