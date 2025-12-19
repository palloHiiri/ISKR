package com.fuzis.search.util;

import com.fuzis.search.transfer.IStateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class HttpUtil
{
    public <T extends IStateDTO> ResponseEntity<T> handleServiceResponse(T res){
        return switch (res.getState()) {
            case OK -> new ResponseEntity<>(res, HttpStatus.OK);
            case Fail_BadData -> new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            case Fail_NotFound -> new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
            case Fail_Conflict -> new ResponseEntity<>(res, HttpStatus.CONFLICT);
            case Fail -> new ResponseEntity<>(res, HttpStatus.INTERNAL_SERVER_ERROR);
            case Fail_Not_Implemented ->  new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
            case Fail_Expired -> new ResponseEntity<>(res, HttpStatus.GONE);
        };
    }
}
