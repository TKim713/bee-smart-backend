package com.api.bee_smart_backend.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MapData {

    private final ModelMapper modelMapper;

    @Autowired
    public MapData(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        // modelMapper.getConfiguration().setAmbiguityIgnored(true); // Bỏ comment nếu cần
    }

    public <T, S> S mapOne(T data, Class<S> type) {
        return modelMapper.map(data, type);
    }

    public <D, T> List<D> mapList(List<T> typeList, Class<D> outClass) {
        return typeList.stream()
                .map(entity -> mapOne(entity, outClass))
                .collect(Collectors.toList());
    }
}
