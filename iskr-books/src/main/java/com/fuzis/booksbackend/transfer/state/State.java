package com.fuzis.booksbackend.transfer.state;

public enum State {
    OK,
    Fail,
    Fail_BadData,
    Fail_NotFound,
    Fail_Conflict,
    Fail_Forbidden, 
    Fail_Not_Implemented,
    Fail_Expired
}