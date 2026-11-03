package com.jacobsmolik.smsguard;

interface ISmsDeleter {
    int deleteById(long id);
    int deleteByAddress(String address);
}
