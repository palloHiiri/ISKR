package com.fuzis.search.transfer;

import com.fuzis.search.transfer.state.State;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChangeDTO<T> implements Serializable, IStateDTO{
    private State state;
    private String message;
    @Nullable
    T key;
}
