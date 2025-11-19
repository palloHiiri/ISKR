package com.fuzis.integrationbus.transfer;

import com.fuzis.integrationbus.transfer.state.State;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SelectDTO <T> implements Serializable, IStateDTO{
    private State state;
    @Nullable
    private T data;
    private String error;
}
