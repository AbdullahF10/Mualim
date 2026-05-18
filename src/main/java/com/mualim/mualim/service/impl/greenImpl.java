package com.mualim.mualim.service.impl;

import com.mualim.mualim.service.Green;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;

@Service
public class greenImpl implements Green {

    @Override
    public String print() {return "green";}

}
