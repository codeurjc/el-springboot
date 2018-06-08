package io.elastest.util;

import java.util.List;
import java.util.Map;

public class Utils {

    @SuppressWarnings({ "unchecked" })
    // Example: If wants name value of {container: {name: value}}, tree should
    // be [docker,container,name]
    public static Object getMapFieldByTreeList(Map<String, Object> dataMap,
            List<String> tree) {
        if (tree.size() > 0) {
            String field = tree.get(0);

            if (dataMap.get(field) != null) {
                if (tree.size() > 1) {
                    List<String> subTree = tree.subList(1, tree.size());
                    return getMapFieldByTreeList(
                            (Map<String, Object>) dataMap.get(field), subTree);
                } else {
                    return dataMap.get(field);
                }

            }

        }
        return null;

    }

}
