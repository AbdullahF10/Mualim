package com.mualim.mualim.service.impl;

import com.mualim.mualim.service.Blue;
import org.springframework.stereotype.Service;

@Service
public class blueImpl implements Blue {
    @Override
    public String print() {return "blue";}
}
