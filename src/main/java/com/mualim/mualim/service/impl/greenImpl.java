package com.mualim.mualim.service.impl;

import com.mualim.mualim.service.Green;
import org.springframework.stereotype.Service;

@Service
public class greenImpl implements Green {

    @Override
    public String print() {return "green";}

}
