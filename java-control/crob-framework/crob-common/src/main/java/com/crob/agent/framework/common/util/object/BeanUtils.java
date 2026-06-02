package com.crob.agent.framework.common.util.object;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BeanUtils {

    public static <T> T toBean(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean copy failed", e);
        }
    }

    public static <T> List<T> toBean(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null) {
            return new ArrayList<>();
        }
        return sourceList.stream()
                .map(source -> toBean(source, targetClass))
                .collect(Collectors.toList());
    }
}
