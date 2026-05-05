package com.mualim.mualim.service.impl;

import com.mualim.mualim.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl {

   private final UserVO userVo;

    public UserServiceImpl(UserVO userVo) {
        this.userVo = userVo;
    }

    public UserVO getUser(int id){

       return userVo;
   }
}
